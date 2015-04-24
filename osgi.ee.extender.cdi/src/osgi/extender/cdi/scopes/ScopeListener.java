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
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
 * @author Arie van Wijngaarden
 */
public class ScopeListener implements ServletRequestListener, HttpSessionListener {
    private static final String LASTVIEW = ScopeListener.class.getName() + ".lastView";
    private ServiceTracker<ExtenderContext, ExtenderContext> tracker;
    
    private static boolean isNewViewMatch(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String lastView = (String) session.getAttribute(LASTVIEW);
        String viewId = request.getPathInfo();
        if (viewId == null) viewId = "";
        if (lastView != null) {
            // Check if this view is the same.
            if (lastView.equals(viewId)) 
                return false;
            // Check whether we should skip this as a new view.
            String viewMatch = request.getServletContext().getInitParameter("osgi.extender.cdi.scopes.newview");
            if (viewMatch == null) {
                viewMatch = ".*";
            }
            if (!Pattern.matches(viewMatch, viewId)) 
                return false;
        }
        session.setAttribute(LASTVIEW, viewId);
        return true;
    }
    
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

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        doWithContext(session.getServletContext(), SessionScoped.class,
                (c) -> c.add(session));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        ServletContext context = session.getServletContext();
        doWithContext(context, SessionScoped.class,
                (c) -> c.remove(session));
        doWithContext(context, ViewScoped.class, (c) -> c.remove(session));
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
        ServletContext context = request.getServletContext();
        HttpSession session = request.getSession(true);
        doWithContext(context, RequestScoped.class, 
                (c) -> {c.add(request); c.setCurrent(request);});
        doWithContext(context, SessionScoped.class, 
                (c) -> c.setCurrent(session));
        // Check if it is a new view. Note that isNewViewMatch is not
        // stateless and as such we call it only once and not in the lambda.
        final boolean isNewView = isNewViewMatch(request);
        doWithContext(context, ViewScoped.class, 
                (c) -> {
                    if (isNewView) {
                        c.remove(session);
                        c.add(session); 
                    }
                    c.setCurrent(session);
                });
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
