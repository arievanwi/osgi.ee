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
package osgi.extender.web.service;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import osgi.extender.web.WebContextDefinition;
import osgi.extender.web.servlet.DispatchingServlet;

/**
 * Component that listens for web context definitions to come up. These definitions can either be defined through normal
 * OSGi services or as a result of a WAB definition header in a bundle (which is picked up elsewhere in this bundle).
 * The listener registers a dispatching servlet at a standard http service to delegate all handling of a web context via
 * the components and classes in this bundle. The servlet is the main entry point for all requests made to the web context.
 */
@Component
public class WebContextListener {
    private HttpService httpService;
    private ServiceTracker<WebContextDefinition, Context> tracker;

    @Activate
    void activate(BundleContext context) {
        // Track the web context definitions.
        tracker = new ServiceTracker<>(context, WebContextDefinition.class,
                new ServiceTrackerCustomizer<WebContextDefinition, Context>() {
            @Override
            public Context addingService(ServiceReference<WebContextDefinition> ref) {
                // Get the service.
                WebContextDefinition definition = context.getService(ref);
                if (definition == null) {
                    return null;
                }
                // Construct the servlet from this service.
                Context ctx = create(ref.getBundle(), definition);
                return ctx;
            }
            @Override
            public void modifiedService(ServiceReference<WebContextDefinition> ref, Context ctxt) {
            }
            @Override
            public void removedService(ServiceReference<WebContextDefinition> ref, Context ctxt) {
                destroy(ctxt);
            }
        });
        new Thread(() -> tracker.open()).start();
    }

    @Deactivate
    void destroy() {
        tracker.close();
    }

    @Reference
    void bindHttpService(HttpService service) {
        httpService = service;
    }

    private static String path(ServletContext c) {
        return c.getContextPath() + "/*";
    }

    /**
     * Create a context/servlet for a specific web context definition.
     *
     * @param bundle The bundle that originally registered the context definition
     * @param def The definition itself
     * @return A context or null if a severe error occurred
     */
    Context create(Bundle bundle, WebContextDefinition def) {
        try {
            DispatchingServlet servlet = ServletContextParser.create(bundle, def);
            httpService.registerServlet(path(servlet.getServletContext()), servlet, null, null);
            return new Context(servlet);
        } catch (Throwable exc) {
            exc.printStackTrace();
            return null;
        }
    }

    /**
     * Destroy a context. Normally done when a bundle stops or a service is unregistered.
     *
     * @param context The context to destroy
     */
    void destroy(Context context) {
        try {
            httpService.unregister(path(context.servlet.getServletContext()));
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    static class Context {
        final DispatchingServlet servlet;
        Context(DispatchingServlet s) {
            servlet = s;
        }
    }
}
