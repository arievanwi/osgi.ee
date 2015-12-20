/*
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
package osgi.extender.web.servlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

import osgi.extender.helpers.DelegatingClassLoader;
import osgi.extender.web.servlet.support.DynamicFRegistration;
import osgi.extender.web.servlet.support.DynamicSRegistration;


/**
 * Implementation of a servlet context that maintains the information for our delegating servlet. It manages all
 * context specific actions, like filter and servlet management, event handling, etc.
 */
public class OurServletContext implements ServletContext {
    private ServletContext delegate;
    private Bundle owner;
    private ClassLoader classLoader;
    private String context;
    private String contextName;
    private String resourceBase;
    private int maxInactive;
    private Map<String, String> initParameters = new HashMap<>();
    private Map<String, Object> attributes = new HashMap<>();
    private Map<String, DynamicFRegistration> filters = new HashMap<>();
    private Map<String, DynamicSRegistration> servlets = new HashMap<>();
    private Collection<EventListener> listeners = new ArrayList<>();
    private ServiceTracker<EventListener, EventListener> tracker;

    public OurServletContext(Bundle bundle, String context, String resourceBase) {
        owner = bundle;
        classLoader = DelegatingClassLoader.from(bundle);
        this.context = context;
        this.resourceBase = resourceBase;
        if (resourceBase != null && resourceBase.endsWith("/")) {
            this.resourceBase = resourceBase.substring(0, resourceBase.length() - 1);
        }
        setAttribute("osgi-bundlecontext", bundle.getBundleContext());
    }

    void init(ServletContext parent) {
        BundleContext bc = getOwner().getBundleContext();
        Collection<Class<? extends EventListener>> toTrack = Arrays.asList(HttpSessionListener.class,
                ServletRequestListener.class, HttpSessionAttributeListener.class, ServletRequestAttributeListener.class,
                ServletContextListener.class);
        Collection<String> objectFilters = toTrack.stream().map((c) -> "(" + Constants.OBJECTCLASS + "=" + c.getName() + ")").collect(Collectors.toList());
        String filterString = "(&" + String.join("", objectFilters) + ")";
        try {
            tracker = new ServiceTracker<>(bc, bc.createFilter(filterString), null);
            tracker.open();
        } catch (Exception exc) {
            exc.printStackTrace();   // Won't happen
        }
        delegate = parent;
        // Initialize the filters.
        filters.values().forEach((f) -> {
            try {
                f.getObject().init(new RegistrationConfig(f, this));
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        });
        // And the servlets.
        servlets.values().forEach((s) -> {
            try {
                s.getObject().init(new RegistrationConfig(s, this));
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        });
        ServletContextEvent event = new ServletContextEvent(this);
        call(ServletContextListener.class, (l) -> l.contextInitialized(event));
    }

    void destroy() {
        ServletContextEvent event = new ServletContextEvent(this);
        call(ServletContextListener.class, (l) -> l.contextDestroyed(event));
    }

    private static <T> T instance(Class<? extends T> clz) {
        try {
            T constructed = clz.newInstance();
            // Perform any resource injection.
            return constructed;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private static <T, O> O instance(Class<? extends T> clz, Function<T, O> function) {
        T constructed = instance(clz);
        return function.apply(constructed);
    }

    private <T, O> O instance(String className, Function<T, O> function) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> clz = (Class<? extends T>) classLoader.loadClass(className);
            return instance(clz, function);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private Function<Filter, DynamicFRegistration> filterAdder(String name) {
        return (f) -> {
            if (delegate != null) {
                throw new IllegalStateException("cannot add filters when initialization is complete");
            }
            DynamicFRegistration reg = new DynamicFRegistration();
            reg.setObject(f);
            reg.setName(name);
            filters.put(name, reg);
            return reg;
        };
    }

    @Override
    public DynamicFRegistration addFilter(String name, String clz) {
        return instance(clz, filterAdder(name));
    }

    @Override
    public DynamicFRegistration addFilter(String name, Filter filter) {
        return filterAdder(name).apply(filter);
    }

    @Override
    public DynamicFRegistration addFilter(String name, Class<? extends Filter> clz) {
        return instance(clz, filterAdder(name));
    }

    private <T extends EventListener> Function<T, Void> listenerAdder() {
        return (l) -> {
            listeners.add(l);
            return null;
        };
    }

    @Override
    public <T extends EventListener> void addListener(T listener) {
        listenerAdder().apply(listener);
    }

    @Override
    public void addListener(Class<? extends EventListener> clz) {
        instance(clz, listenerAdder());
    }

    @Override
    public void addListener(String clzName) {
        instance(clzName, listenerAdder());
    }

    private Function<Servlet, DynamicSRegistration> servletAdder(String name) {
        return (s) -> {
            if (delegate != null) {
                throw new IllegalStateException("cannot add servlets when initialization is complete");
            }
            DynamicSRegistration reg = new DynamicSRegistration();
            reg.setObject(s);
            reg.setName(name);
            servlets.put(name, reg);
            return reg;
        };
    }

    @Override
    public DynamicSRegistration addServlet(String name, String clz) {
        return instance(clz, servletAdder(name));
    }

    @Override
    public DynamicSRegistration addServlet(String name, Servlet servlet) {
        return servletAdder(name).apply(servlet);
    }

    @Override
    public DynamicSRegistration addServlet(String name, Class<? extends Servlet> clz) {
        return instance(clz, servletAdder(name));
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clz) throws ServletException {
        return instance(clz);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clz) throws ServletException {
        return instance(clz);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clz) throws ServletException {
        return instance(clz);
    }

    @Override
    public void declareRoles(String... roles) {
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public ServletContext getContext(String path) {
        return delegate.getContext(path);
    }

    @Override
    public String getContextPath() {
        return context;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 1;
    }

    @Override
    public int getMajorVersion() {
        return 3;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String s) {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String name) {
        return filters.get(name);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Collections.unmodifiableMap(filters);
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String disp) {
        return null;
    }

    @Override
    public String getRealPath(String virt) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public URL getResource(String name) throws MalformedURLException {
        return new URL(owner.getResource(resourceBase), name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        try {
            URL url = getResource(name);
            return url.openStream();
        } catch (Exception exc) {
            return null;
        }
    }

    @Override
    public Set<String> getResourcePaths(String rel) {
        String start = resourceBase;
        if (!rel.startsWith("/")) {
            start += "/";
        }
        start += rel;
        Enumeration<URL> found = owner.findEntries(start, null, false);
        if (found == null) {
            return null;
        }
        return Collections.list(found).stream().
                map((u) -> u.getPath()).
                map((s) -> s.substring(resourceBase.length())).
                filter((s) -> !s.contains("WEB-INF") && !s.contains("META-INF")).
                collect(Collectors.toSet());
    }

    @Override
    public String getServerInfo() {
        return "OSGi/extender";
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        return servlets.get(name).getObject();
    }

    @Override
    public String getServletContextName() {
        return contextName;
    }

    @Override
    public Enumeration<String> getServletNames() {
        return Collections.enumeration(Collections.unmodifiableCollection(servlets.keySet()));
    }

    @Override
    public ServletRegistration getServletRegistration(String name) {
        return servlets.get(name);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Collections.unmodifiableMap(servlets);
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return Collections.enumeration(servlets.values().stream().map((r) -> r.getObject()).collect(Collectors.toList()));
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return delegate.getSessionCookieConfig();
    }

    @Override
    public String getVirtualServerName() {
        return delegate.getVirtualServerName();
    }

    @Override
    public void log(String s) {
        delegate.log(s);
    }

    @Deprecated
    @Override
    public void log(Exception exc, String message) {
        delegate.log(exc, message);
    }

    @Override
    public void log(String string, Throwable thr) {
        delegate.log(string, thr);
    }

    @Override
    public void removeAttribute(String attr) {
        ServletContextAttributeEvent event = new ServletContextAttributeEvent(this, attr, attributes.remove(attr));
        call(ServletContextAttributeListener.class, (l) -> l.attributeRemoved(event));
    }

    @Override
    public void setAttribute(String attr, Object value) {
        Object original = attributes.get(attr);
        attributes.put(attr, value);
        ServletContextAttributeEvent event = new ServletContextAttributeEvent(this, attr, value);
        if (original != null) {
            call(ServletContextAttributeListener.class, (l) -> l.attributeReplaced(event));
        }
        else {
            call(ServletContextAttributeListener.class, (l) -> l.attributeAdded(event));
        }
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public boolean setInitParameter(String param, String value) {
        if (initParameters.containsKey(param) || value == null) {
            return false;
        }
        initParameters.put(param, value);
        return true;
    }

    @Override
    public String getInitParameter(String parameterName) {
        return initParameters.get(parameterName);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(Collections.unmodifiableCollection(initParameters.keySet()));
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> modes) {
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return delegate.getDefaultSessionTrackingModes();
    }

    void setMaxInactive(int v) {
        maxInactive = v;
    }

    int getMaxInactive() {
        return maxInactive;
    }

    <T extends EventListener> void call(Class<T> type, Consumer<T> cons) {
        List<EventListener> allListeners = new ArrayList<>();
        allListeners.addAll(listeners);
        if (tracker != null) {
            allListeners.addAll(tracker.getTracked().values());
        }
        allListeners.stream().filter((l) -> type.isAssignableFrom(l.getClass())).map((l) -> type.cast(l)).forEach((l) -> {
            try {
                cons.accept(l);
            } catch (Exception exc) {
                log("could not perform call on: " + l, exc);
            }
        });
    }

    void setDelegate(ServletContext parent) {
        delegate = parent;
    }

    Bundle getOwner() {
        return owner;
    }

    FilterChain getChain(String path, StringBuffer servletPath) {
        return ChainCalculator.getChain(filters, servlets, path, servletPath);
    }

    @Override
    public String toString() {
        return "OSGi WAB Extender servlet context for \"" + getContextPath() + "\"";
    }
}