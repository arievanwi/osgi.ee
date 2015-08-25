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
package osgi.extender.cdi.weld;

import java.util.Hashtable;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import osgi.extender.cdi.Helper;

/**
 * Bundle listener that takes care of starting and stopping the CDI container for specific bundles.
 * This is the main entry point for CDI container creation for bundles that are extended by this 
 * extender. Only bundles that have a capability requirement for osgi.extender=osgi.cdi will be
 * processed by us.
 * 
 * @author Arie van Wijngaarden
 */
public class CdiBundleChangeListener implements BundleTrackerCustomizer<Object> {
    private Bundle me;
    
    public CdiBundleChangeListener(Bundle me) {
        this.me = me;
    }
    
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        // Get the wiring of the bundle.
        BundleWiring wiring = Helper.getWiring(bundle);
        // See it is wired to us as extender.
        List<BundleWire> requirements = wiring.getRequiredWires("osgi.extender");
        Context context = null;
        if (requirements != null &&
            requirements.stream().anyMatch((w) -> w.getProviderWiring().getBundle().equals(me))) {
            // Create the stuff.
            WeldContainer container = new WeldContainer(bundle);
            Hashtable<String, Object> dict = new Hashtable<>();
            dict.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
            String cat = bundle.getHeaders().get(Constants.BUNDLE_CATEGORY);
            if (cat != null) {
                dict.put(Constants.BUNDLE_CATEGORY, cat);
            }
            ServiceRegistration<BeanManager> reg = bundle.getBundleContext().registerService(BeanManager.class, 
                container.getManager(), dict);
            context = new Context(container, reg);
        }
        return context;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object context) {
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object c) {
        if (c != null) {
            Context context = (Context) c;
            try {
                context.registration.unregister();
            } catch (Exception exc) {}
            context.container.destroy();
        }
    }

    static class Context {
        final WeldContainer container;
        final ServiceRegistration<?> registration;
        Context(WeldContainer container, ServiceRegistration<?> registration) {
            this.container = container;
            this.registration = registration;
        }
    }
}
