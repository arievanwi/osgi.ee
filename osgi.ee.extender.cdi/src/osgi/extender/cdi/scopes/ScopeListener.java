/*
 * Copyright 2015, Imtech Traffic & Infra
 * Copyright 2015, aVineas IT Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package osgi.extender.cdi.scopes;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import osgi.cdi.annotation.ViewScoped;

/**
 * Listener for scope issues. Tracks sessions and requests and acts on registered services accordingly.
 * This listener should be registered with web applications as listener to make sure that
 * the various scopes are correctly set before CDI processing takes place.
 *
 * Note: this listener *must* be defined for a web application to make CDI working correctly with other
 * scopes that default
 *
 * @author Arie van Wijngaarden
 */
public class ScopeListener implements ServletRequestListener, HttpSessionListener {
    public static final String SCOPELISTENER = ScopeListener.class.getName() + ".instance";
    private ServiceTracker<ExtenderContext, ExtenderContext> tracker;

    /**
     * Perform an action on all extender contexts that match a specific scope.
     *
     * @param context The servlet context. Needed to register a service tracker
     * @param scope The scope the action applies
     * @param consumer The consumer to execute on the found services
     */
    private void doWithContext(ServletContext context,
            Class<? extends Annotation> scope, Consumer<ExtenderContext> consumer) {
        synchronized (this) {
            if (tracker == null) {
                // First try to use the bundle context from the web extender attribute.
                BundleContext bc = (BundleContext) context.getAttribute("osgi-bundlecontext");
                if (bc == null) {
                    // Not running in a standard Web extender.
                    bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
                }
                tracker = new ServiceTracker<>(bc, ExtenderContext.class, null);
                tracker.open();
            }
        }
        // Stream: filter on the mentioned scope type and execute the consumer on it
        tracker.getTracked().values().stream().
        filter((e) -> scope.equals(e.getScope())).
        forEach(consumer);
    }

    /**
     * Get the view identifier from a servlet request.
     *
     * @param request The servlet request
     * @return The view identifier
     */
    private static String getViewId(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path != null && path.endsWith("/")) {
            path = null;
        }
        return path;
    }

    /**
     * Get the management identifier for a specific view/session combination.
     *
     * @param session The session to get the identifier for
     * @param viewId The view identifier to get the the management key for
     * @return A combination of session and view identifier. The view identifier comes first
     * so it is possible to group identifiers by checking their first part
     */
    private static String getViewIdentifier(HttpSession session, String viewId) {
        if (viewId == null) {
            return null;
        }
        return session.getId() + "-" + viewId;
    }

    /**
     * Get the object identifiers for a specific session from an extender context.
     *
     * @param context The context to get the identifiers from
     * @param session The session it applies
     * @return A list with object identifiers
     */
    private static Collection<Object> identifiersThisSession(ExtenderContext context, HttpSession session) {
        String ident = getViewIdentifier(session, "");
        return context.getIdentifiers().stream().filter((i) -> i.toString().startsWith(ident)).collect(Collectors.toList());
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        doWithContext(session.getServletContext(), SessionScoped.class,
                (c) -> c.add(session.getId()));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        ServletContext context = session.getServletContext();
        doWithContext(context, SessionScoped.class,
                (c) -> c.remove(session.getId()));
        doWithContext(context, ViewScoped.class, (c) -> {
            identifiersThisSession(c, session).forEach((i) -> {
                c.remove(i);
            });
        });
    }

    /**
     * Return a consumer for an extender context that is able to set the active context for a view scope.
     *
     * @param request The request we are busy with
     * @param isNewView Indication whether a new view scope must be created
     * @return A consumer that sets the active view scope context
     */
    private static Consumer<ExtenderContext> setCurrent(HttpServletRequest request, boolean isNewView) {
        HttpSession session = request.getSession(true);
        String id = getViewIdentifier(session, getViewId(request));
        String keepViews = request.getServletContext().getInitParameter("osgi.extender.cdi.scopes.views");
        int nv = 10;
        if (keepViews != null) {
            nv = Integer.parseInt(keepViews);
        }
        final int numberOfViews = nv;

        return (c) -> {
            if (id == null) {
                return;
            }
            if (isNewView) {
                // Check the number of identifiers that are in the scope.
                // This must be limited to the max. number of pages open.
                Collection<Object> identifiers = identifiersThisSession(c, session);
                int toDelete = identifiers.size() - numberOfViews;
                Iterator<Object> it = identifiers.iterator();
                while (toDelete > 0) {
                    c.remove(it.next());
                    toDelete--;
                }
                c.remove(id);
                c.add(id);
            }
            c.setCurrent(id);
        };
    }

    /**
     * Set the view scope for the current thread. This method is called during request initialization, but
     * may also be called to reset the view scope from other parts using a reference to this instance via
     * the appropriate request attribute.
     *
     * @param request The servlet request
     * @param isNewView Indication whether a new view scope must be forced
     */
    public void setViewScope(HttpServletRequest request, boolean isNewView) {
        ServletContext context = request.getServletContext();
        doWithContext(context, ViewScoped.class, setCurrent(request, isNewView));
    }

    /**
     * Set a fallback view scope in case there is none defined (for example for resources or outside JSF).
     *
     * @param request The servlet request
     */
    private void setFallbackViewScope(HttpServletRequest request) {
        final String referer = request.getHeader("Referer");
        final String id = getViewIdentifier(request.getSession(), getViewId(request));
        int subLength = getViewIdentifier(request.getSession(), "").length();
        HttpSession session = request.getSession();
        Consumer<ExtenderContext> createNew = setCurrent(request, true);
        doWithContext(request.getServletContext(), ViewScoped.class, (c) -> {
            if (c.getIdentifiers().contains(id)) {
                c.setCurrent(id);
            }
            else if (referer != null) {
                // Check if the referer exists in the identifiers.
                Optional<Object> ident = identifiersThisSession(c, session).stream().
                        filter((i) -> referer.contains(i.toString().substring(subLength))).findAny();
                if (ident.isPresent()) {
                    c.setCurrent(ident.get());
                }
            }
            else {
                createNew.accept(c);
            }
        });
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
        request.setAttribute(SCOPELISTENER, this);
        ServletContext context = request.getServletContext();
        HttpSession session = request.getSession(true);
        doWithContext(context, RequestScoped.class,
                (c) -> {c.add(request); c.setCurrent(request);});
        doWithContext(context, SessionScoped.class,
                (c) -> c.setCurrent(session.getId()));
        setFallbackViewScope(request);
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
        ServletContext context = request.getServletContext();
        doWithContext(context, SessionScoped.class, (c) -> c.setCurrent(null));
        doWithContext(context, ViewScoped.class, (c) -> c.setCurrent(null));
        doWithContext(context, RequestScoped.class, (c) -> c.remove(request));
    }
}
