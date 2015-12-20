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

import java.io.IOException;
import java.util.Hashtable;
import java.util.ServiceLoader;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

/**
 * Servlet that handles the dispatching of a specific context to filters and servlets. It takes care of initialization of
 * the context and dispatching incoming requests to the correct chain.
 */
public class DispatchingServlet implements Servlet {
    private ServletConfig baseConfig;
    private OurServletContext servletContext;
    private ServiceRegistration<ServletContext> registration;

    public DispatchingServlet(OurServletContext ctx) {
        servletContext = ctx;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        baseConfig = config;
        // Do the initialization stuff.
        callInitializers(servletContext, servletContext.getClassLoader());
        servletContext.init(config.getServletContext());
        registration = registerService(servletContext);
        servletContext.log("context \"" + servletContext.getContextPath() + "\" created");
    }

    @Override
    public void destroy() {
        try {
            registration.unregister();
        } catch (Exception exc) {}
        // Send the context destroyed event to all applicable listeners.
        servletContext.destroy();
        servletContext.log("context \"" + servletContext.getContextPath() + "\" destroyed");
    }

    @Override
    public ServletConfig getServletConfig() {
        return baseConfig;
    }

    @Override
    public String getServletInfo() {
        return "OSGi/JEE dispatcher servlet";
    }

    public OurServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void service(ServletRequest r, ServletResponse resp) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) r;
        HttpServletResponse response = (HttpServletResponse) resp;
        String path = request.getRequestURI();
        // Match it against the context.
        String subpath = path.substring(servletContext.getContextPath().length());
        if (subpath.length() == 0) {
            response.sendRedirect(path + "/");
            return;
        }
        StringBuffer servletPath = new StringBuffer();
        FilterChain chain = servletContext.getChain(subpath, servletPath);
        if (chain == null) {
            servletContext.log("no servlet mapping found for \"" + subpath + "\", context: " + servletContext.getContextPath());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String pathInfo = subpath.substring(servletPath.length());
        OurServletRequest req = new OurServletRequest(request, servletContext, servletPath.toString(), pathInfo);
        chain.doFilter(req, response);
    }

    /**
     * Call the initializers accessible from the specific class loader. This may result in additional changes to
     * the servlet context because the initializers may want to add event listeners or maybe even servlets to
     * the context.
     *
     * @param context The servlet context to initialize
     * @param loader The class loader
     */
    private static void callInitializers(ServletContext context, ClassLoader loader) {
        ServiceLoader<ServletContainerInitializer> services =  ServiceLoader.load(ServletContainerInitializer.class, loader);
        services.forEach((l) -> {
            try {
                l.onStartup(null, context);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        });
    }

    /**
     * Register a service for the servlet context according to the WAB specification.
     *
     * @param context The context to register the service for
     * @return The service registration
     */
    private static ServiceRegistration<ServletContext> registerService(OurServletContext context) {
        Hashtable<String, Object> dict = new Hashtable<>();
        Bundle bundle = context.getOwner();
        dict.put("osgi.web.symbolicname", bundle.getSymbolicName());
        Version version = bundle.getVersion();
        if (version != null) {
            dict.put("osgi.web.version", version);
        }
        dict.put("osgi.web.contextpath", context.getContextPath());
        ServiceRegistration<ServletContext> registration = context.getOwner().getBundleContext().registerService(ServletContext.class, context, dict);
        return registration;
    }
}
