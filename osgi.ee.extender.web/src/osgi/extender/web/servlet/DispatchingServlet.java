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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
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
    interface Runner {
        void run() throws ServletException, IOException;
    }
    private ServletConfig baseConfig;
    private OurServletContext servletContext;
    private ServiceRegistration<ServletContext> registration;
    private Collection<String> welcomePages;
    private Map<Integer, String> errorPages;
    private Map<Class<?>, String> exceptionPages;

    public DispatchingServlet(OurServletContext ctx, Collection<String> welcomes,
            Map<Integer, String> errorPages, Map<Class<?>, String> exceptionPages) {
        servletContext = ctx;
        welcomePages = welcomes;
        this.errorPages = errorPages;
        this.exceptionPages = exceptionPages;
    }

    private void doWithClassLoader(Runner actions) throws ServletException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(servletContext.getClassLoader());
            actions.run();
        } catch (IOException exc) {
            throw new ServletException(exc);
        }
        finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        baseConfig = config;
        // Do the initialization stuff.
        doWithClassLoader(() -> {
            callInitializers(servletContext, servletContext.getClassLoader());
            servletContext.init(config.getServletContext());
        });
        registration = registerService(servletContext);
        servletContext.log("context \"" + servletContext.getContextPath() + "\" initialized");
    }

    @Override
    public void destroy() {
        try {
            registration.unregister();
        } catch (Exception exc) {}
        // Send the context destroyed event to all applicable listeners.
        try {
            doWithClassLoader(() -> servletContext.destroy());
        } catch (Exception exc) {
            exc.printStackTrace();
        }
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
        // Start of path? Redirect to root.
        if (subpath.length() == 0) {
            response.sendRedirect(path + "/");
            return;
        }
        // Is it the start and do we have welcome pages? Redirect to first page.
        if (subpath.equals("/") && welcomePages.size() > 0) {
            String welcomeFile = welcomePages.iterator().next();
            URL welcomePath = servletContext.getResource(welcomeFile);
            if (welcomePath == null) {
                // No resource. Just redirect to let it be handled by a servlet.
                response.sendRedirect(servletContext.getContextPath() + welcomeFile);
            }
            else {
                // Copy the file to the output.
                try (InputStream in = welcomePath.openStream();
                     OutputStream out = response.getOutputStream()){
                    byte[] data = new byte[200];
                    int size;
                    while ((size = in.read(data)) > 0) {
                        out.write(data, 0, size);
                    }
                }
            }
            return;
        }
        // Use automatic handling of error pages from now on.
        OurServletResponse res = new OurServletResponse(response, servletContext.getContextPath(), errorPages);
        StringBuffer servletPath = new StringBuffer();
        FilterChain chain = servletContext.getChain(subpath, servletPath);
        if (chain == null) {
            servletContext.log("no servlet mapping found for \"" + subpath +
                    "\", context: " + servletContext.getContextPath());
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String pathInfo = subpath.substring(servletPath.length());
        OurServletRequest req = new OurServletRequest(request, servletContext, servletPath.toString(), pathInfo);
        // Put down the chain.
        ServletRequestEvent event = new ServletRequestEvent(servletContext, req);
        try {
            servletContext.call(ServletRequestListener.class, (l) -> l.requestInitialized(event));
            doWithClassLoader(() -> chain.doFilter(req, res));
        } catch (ServletException exc) {
            servletContext.log("exception while handling " + subpath, exc);
            Throwable root = exc;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            Class<?> throwedClass = root.getClass();
            Optional<String> errorPage =
                    exceptionPages.entrySet().stream().
                    filter((e) -> e.getKey().isAssignableFrom(throwedClass)).
                    map((e) -> e.getValue()).findFirst();
            if (!errorPage.isPresent()) {
                throw exc;
            }
            response.sendRedirect(servletContext.getContextPath() + errorPage.get());
        }
        finally {
            servletContext.call(ServletRequestListener.class, (l) -> l.requestDestroyed(event));
        }
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
        ServiceLoader<ServletContainerInitializer> services =
                ServiceLoader.load(ServletContainerInitializer.class, loader);
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
        ServiceRegistration<ServletContext> registration =
                context.getOwner().getBundleContext().registerService(ServletContext.class, context, dict);
        return registration;
    }
}
