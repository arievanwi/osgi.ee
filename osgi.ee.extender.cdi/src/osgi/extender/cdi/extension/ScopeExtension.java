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
package osgi.extender.cdi.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import osgi.cdi.annotation.ComponentScoped;
import osgi.cdi.annotation.ViewScoped;
import osgi.extender.cdi.extension.context.AbstractContext;
import osgi.extender.cdi.extension.context.BasicContext;
import osgi.extender.cdi.extension.context.MultiInstanceContext;
import osgi.extender.cdi.scopes.ExtenderContext;

/**
 * Extension that takes care of the scope stuff. It registers the various scopes and
 * exports the scopes as service to be handled by the servlet handling (for example) to
 * start the scope at the correct time.
 * 
 * @author Arie van Wijngaarden
 */
public class ScopeExtension implements Extension {
    private BundleContext context;
    private Collection<AbstractContext> contexts;                 // The scope contexts.
    private Collection<ServiceRegistration<?>> registrations;     // Service registration of our scopes

    /**
     * Constructor to save the bundle context, used for service registration.
     * 
     * @param context The context
     */
    public ScopeExtension(BundleContext context) {
        this.context = context;
        this.contexts = new ArrayList<>();
        this.registrations = new ArrayList<>();
    }
    
    /**
     * Before bean discovery. Adds the bundle and view scopes, nothing more.
     * 
     * @param event The event
     */
    @SuppressWarnings("static-method")
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event) {
        event.addScope(ComponentScoped.class, false, false);
        event.addScope(ViewScoped.class, true, true);
    }

    /**
     * Save a created context in our own list, to allow for clean-up later.
     * 
     * @param cont The context to add
     * @return The same context
     */
    private AbstractContext addContext(AbstractContext cont) {
        this.contexts.add(cont);
        return cont;
    }
    
    /**
     * Handling of the after bean discovery event as fired by the bean manager. The handling creates
     * contexts for the session and request scopes.
     * 
     * @param event The event that can be used for the actions
     */
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        // Register the session scope context.
        event.addContext(registerContext(addContext(new MultiInstanceContext(SessionScoped.class, null))));
        // And the request scope context.
        event.addContext(registerContext(addContext(new MultiInstanceContext(RequestScoped.class, null))));
        // And the bundle scope.
        event.addContext(addContext(new BasicContext(ComponentScoped.class, null)));
        // And the view scope.
        event.addContext(registerContext(addContext(new MultiInstanceContext(ViewScoped.class, null))));
    }
    
    /**
     * Register a context as service so it can be found at a later stage when notifications must occur
     * on them. Internal method.
     * 
     * @param cont The context
     * @return The same context for piping the result
     */
    private AbstractContext registerContext(AbstractContext cont) {
        Hashtable<String, String> properties = new Hashtable<>();
        // Put the scope type on it as property. Just to see what they are, not really needed.
        properties.put("scope", cont.getScope().getSimpleName());
        ServiceRegistration<?> reg = this.context.registerService(ExtenderContext.class.getName(), cont, properties);
        this.registrations.add(reg);
        return cont;
    }
    
    /**
     * Destroy this extension. Removes the scope services.
     */
    public void destroy(@Observes BeforeShutdown shut) {
        this.contexts.forEach((c) -> c.destroy());
        this.registrations.stream().
            forEach((s) ->  {
                try {
                    s.unregister();
                } catch (Exception exc) {}
            });
    }
}