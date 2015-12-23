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
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
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
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import osgi.extender.helpers.DelegatingClassLoader;
import osgi.extender.web.WebContextDefinition;
import osgi.extender.web.servlet.support.DynamicFRegistration;
import osgi.extender.web.servlet.support.DynamicRegistration;
import osgi.extender.web.servlet.support.DynamicSRegistration;

/**
 * Servlet context that acts on behalf of a web-app definition within a bundle. It performs all actions for
 * a normal servlet context but takes the specific requirements of a bundle/WAB into account. As such it performs additional
 * functionality by calling registered services at specific moments in time, like event handling.
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
    private ServiceTracker<EventListener, EventListener> eventListenerTracker;
    private ServiceTracker<Filter, String> filterTracker;
    private ServiceTracker<Servlet, String> servletTracker;

    /**
     * Base constructor, so specialities
     *
     * @param bundle The bundle this context is defined for. This determines the class loader
     * @param context The context on which this servlet context runs, like "/Test"
     * @param resourceBase The base path in the bundle that is used for resource resolving
     */
    public OurServletContext(Bundle bundle, String context, String resourceBase) {
        owner = bundle;
        classLoader = DelegatingClassLoader.from(bundle);
        this.context = context;
        this.resourceBase = resourceBase;
        if (resourceBase != null && resourceBase.endsWith("/")) {
            this.resourceBase = resourceBase.substring(0, resourceBase.length() - 1);
        }
        // Required for a WAB according to the specification.
        setAttribute("osgi-bundlecontext", bundle.getBundleContext());
    }

    /**
     * Init method called by the main servlet when the wrapping servlet is initialized. This means that the context is
     * taken into service by the system.
     *
     * @param parent The parent servlet context. Just for some delegation actions
     */
    void init(ServletContext parent) {
        // Set up the tracking of event listeners.
        BundleContext bc = getOwner().getBundleContext();
        delegate = parent;
        Collection<Class<? extends EventListener>> toTrack = Arrays.asList(HttpSessionListener.class,
                ServletRequestListener.class, HttpSessionAttributeListener.class, ServletRequestAttributeListener.class,
                ServletContextListener.class);
        Collection<String> objectFilters = toTrack.stream().
                map((c) -> "(" + Constants.OBJECTCLASS + "=" + c.getName() + ")").collect(Collectors.toList());
        String filterString = "|" + String.join("", objectFilters);
        eventListenerTracker = startTracking(filterString,
                new Tracker<EventListener, EventListener>(bc, getContextPath(), (e) -> e, (e) -> {}));
        // Initialize the servlets.
        ServletContextEvent event = new ServletContextEvent(this);
        call(ServletContextListener.class, (l) -> l.contextInitialized(event));
        servlets.values().forEach((s) -> init(s));
        // And the filters.
        filters.values().forEach((f) -> init(f));
        // Set up the tracking of servlets and filters.
        servletTracker = startTracking(Constants.OBJECTCLASS + "=" + Servlet.class.getName(),
                new Tracker<Servlet, String>(bc, getContextPath(), this::addServlet, this::removeServlet));
        filterTracker = startTracking(Constants.OBJECTCLASS + "=" + Filter.class.getName(),
                new Tracker<Filter, String>(bc, getContextPath(), this::addFilter, this::removeFilter));
    }

    /**
     * Destroy the current context. Called by the servlet when it is taken out of service.
     */
    void destroy() {
        new ArrayList<>(filters.values()).forEach(this::destroy);
        new ArrayList<>(servlets.values()).forEach(this::destroy);
        filterTracker.close();
        servletTracker.close();
        ServletContextEvent event = new ServletContextEvent(this);
        call(ServletContextListener.class, (l) -> l.contextDestroyed(event));
        eventListenerTracker.close();
    }

    private <T, C> ServiceTracker<T, C> startTracking(String filter, ServiceTrackerCustomizer<T, C> cust) {
        try {
            BundleContext bc = getOwner().getBundleContext();
            String filterString = "(&(" + filter + ")(" + WebContextDefinition.WEBCONTEXTPATH + "=*))";
            ServiceTracker<T, C> tracker = new ServiceTracker<>(bc, bc.createFilter(filterString), cust);
            tracker.open();
            return tracker;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Create an instance of a class can convert any exceptions into runtime exceptions (since they should never occur).
     *
     * @param clz The class to instantiate
     * @return The created instance
     */
    private static <T> T instance(Class<? extends T> clz) {
        try {
            T constructed = clz.newInstance();
            // Perform any resource injection.
            return constructed;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Create an instance of a class and apply a function on the constructed object. The returned value of the
     * function is returned to the caller.
     *
     * @param clz The class to instantiate
     * @param function The function to execute on it
     * @return The returned value from the function
     */
    private static <T, O> O instance(Class<? extends T> clz, Function<T, O> function) {
        T constructed = instance(clz);
        return function.apply(constructed);
    }

    /**
     * Instance creation from a class name. Is like the other methods above, but first the class is loaded
     * with the constructed class loader of this conttext.
     *
     * @param className The class name to load
     * @param function  The function to apply
     * @return The return value of the function
     */
    private <T, O> O instance(String className, Function<T, O> function) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> clz = (Class<? extends T>) classLoader.loadClass(className);
            return instance(clz, function);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Perform adding a specific web object to a managed set.
     *
     * @param name The name by which the web object is known
     * @param container The map containing the values
     * @param supplier The supplier that converts a string/object combination to a registration object
     * @param init The consumer to invoke for inialization
     * @return The adding function, to be used in the various variants of add...
     */
    private <T, R extends DynamicRegistration<T>> Function<T, R> adderFunction(
            String name, Map<String, R> container, BiFunction<String, T, R> supplier, Consumer<R> init) {
        return (o) -> {
            if (container.containsKey(name)) {
                return null;
            }
            R reg = supplier.apply(name, o);
            container.put(name, reg);
            if (delegate != null) {
                init.accept(reg);
            }
            return reg;
        };
    }

    private void init(DynamicFRegistration filter) {
        try {
            filter.getObject().init(new RegistrationConfig(filter, this));
            log("filter: " + filter + " initialized");
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private void destroy(DynamicFRegistration filter) {
        if (filter == null) {
            return;
        }
        filter.getObject().destroy();
        filters.remove(filter.getName());
        log("filter: " + filter + " initialized");
    }

    /**
     * Function that adds a filter by a specific name and returns the filter registration.
     *
     * @param name The name of the filter
     * @return The function that converts a filter into a dynamic filter registration
     */
    private Function<Filter, DynamicFRegistration> filterAdder(String name) {
        return adderFunction(name, filters, DynamicFRegistration::new, this::init);
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

    private static void doParameters(DynamicRegistration<?> reg, WebInitParam[] param) {
        for (WebInitParam p : param) {
            reg.setInitParameter(p.name(), p.value());
        }
    }

    /**
     * Method invoked for tracking filters that are registered via the OSGi service registry
     *
     * @param filter The filter to add
     * @return The name of the filter, if added
     */
    private String addFilter(Filter filter) {
        Class<?> clz = filter.getClass();
        WebFilter ann = clz.getAnnotation(WebFilter.class);
        if (ann == null) {
            return null;
        }
        String name = ann.filterName();
        DynamicFRegistration reg = addFilter(name, filter);
        if (reg == null) {
            return null;
        }
        reg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, ann.urlPatterns());
        reg.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), false, ann.servletNames());
        doParameters(reg, ann.initParams());
        return name;
    }

    private void removeFilter(String name) {
        destroy(filters.get(name));
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

    private void init(DynamicSRegistration servlet) {
        try {
            servlet.getObject().init(new RegistrationConfig(servlet, this));
            log("servlet: " + servlet + " initialized");
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private void destroy(DynamicSRegistration servlet) {
        if (servlet == null) {
            return;
        }
        servlet.getObject().destroy();
        servlets.remove(servlet.getName());
        log("servlet: " + servlet + " destroyed");
    }

    /**
     * Return a function that converts a servlet into a registration. Is the function that is needed during the various
     * servlet creation methods.
     *
     * @param name The servlet name
     * @return The function to convert a servlet into a registration.
     */
    private Function<Servlet, DynamicSRegistration> servletAdder(String name) {
        return adderFunction(name, servlets, DynamicSRegistration::new, this::init);
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

    private String addServlet(Servlet servlet) {
        Class<?> clz = servlet.getClass();
        WebServlet ann = clz.getAnnotation(WebServlet.class);
        if (ann == null) {
            return null;
        }
        String name = ann.name();
        DynamicSRegistration reg = addServlet(name, servlet);
        if (reg == null) {
            return null;
        }
        doParameters(reg, ann.initParams());
        reg.addMapping(ann.urlPatterns());
        return name;
    }

    private void removeServlet(String name) {
        destroy(servlets.get(name));
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
        return delegate.getMimeType(s);
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
        // We don't support JSP.
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String disp) {
        return delegate.getNamedDispatcher(disp);
    }

    @Override
    public String getRealPath(String virt) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return delegate.getRequestDispatcher(path);
    }

    @Override
    public URL getResource(String name) throws MalformedURLException {
        if (resourceBase == null) {
            return null;
        }
        return owner.getEntry(resourceBase + "/" + name);
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
        delegate.log(toString() + ": " + s);
    }

    @Deprecated
    @Override
    public void log(Exception exc, String message) {
        delegate.log(exc, message);
    }

    @Override
    public void log(String string, Throwable thr) {
        delegate.log(toString() + ": " + string, thr);
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

    public void setMaxInactive(int v) {
        maxInactive = v;
    }

    int getMaxInactive() {
        return maxInactive;
    }

    <T extends EventListener> void call(Class<T> type, Consumer<T> cons) {
        List<EventListener> allListeners = new ArrayList<>();
        allListeners.addAll(listeners);
        if (eventListenerTracker != null) {
            allListeners.addAll(eventListenerTracker.getTracked().values());
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
        return "WebContext " + getContextPath();
    }
}