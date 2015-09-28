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
package osgi.extender.jpa.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Listener for bundle changes that result in an entity manager factory to be created. It actually
 * listens for two parts: a persistence provider listener and a bundle listener. Both may end up
 * in the creation or destruction of persistence units. 
 * 
 * Synchronization requirements: the bundles tracked are synchronized on the listener itself, information for
 * a persistence unit context is synchronized on the context object. 
 */
public class JpaBundleChangeListener implements BundleTrackerCustomizer<Object>,  PPListener {
    private Map<Bundle, List<Context>> bundles;
    private ServiceTracker<PersistenceProvider, PersistenceProvider> tracker;
    private PPProvider provider;
    
    public JpaBundleChangeListener(Bundle me) {
        bundles = new HashMap<>();
        PersistenceProviderListener listener = new PersistenceProviderListener(me.getBundleContext(), this);
        tracker = new ServiceTracker<>(me.getBundleContext(), PersistenceProvider.class, listener);
        provider = listener;
    }
    
    private void create(Bundle bundle, Context context) {
        Map.Entry<String, PersistenceProvider> pp = provider.get(context.definition.provider);
        PersistenceUnitInfo info = PersistenceUnitProcessor.getPersistenceUnitInfo(bundle, 
                context.definition, pp.getValue());
        context.factory = PersistenceUnitProcessor.createFactory(pp.getValue(), info);
        context.usedProvider = pp.getKey();
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(EntityManagerFactoryBuilder.JPA_UNIT_NAME, context.definition.name);
        props.put(PersistenceUnitTransactionType.class.getName(), info.getTransactionType().name());
        // Do the registration of the service asynchronously. Since it may imply all kinds of
        // listening actions performed as a result of it, it may block the bundle handling.
        new Thread(() -> { 
            synchronized (context) {
                context.registration = bundle.getBundleContext().registerService(
                        EntityManagerFactory.class, context.factory, props);
            }
        }).start();
    }
        
    private static void destroy(Context context) {
        synchronized (context) {
            if (context.factory != null)
                context.factory.close();
            context.factory = null;
            try {
                context.registration.unregister();
            } catch (Exception exc) {}
            context.registration = null;
            context.usedProvider = null;
        }
    }
    
    private synchronized void addRegistration(Bundle bundle) {
        List<Context> contexts = bundles.get(bundle);
        if (contexts != null) {
            contexts.stream().
                filter((c) -> c.factory == null).
                filter((c) -> provider.get(c.definition.provider) != null).
                forEach((context) -> create(bundle, context));
        }
    }
    
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        String persistence = bundle.getHeaders().get("Meta-Persistence");
        if (persistence == null) return null;
        // Process the persistence units.
        String[] names = persistence.split(",");
        final Collection<String> units = new LinkedHashSet<>();
        // Start with META-INF/persistence.xml as required by the specification.
        units.add("/META-INF/persistence.xml");
        // Add the existing
        units.addAll(Arrays.asList(names));
        // Every file may have multiple units. So need to flatmap the lot
        List<Context> punits = units.stream().
            map((c) -> c.trim()).
            filter((c) -> c.length() > 0).
            flatMap((name) -> PersistenceUnitProcessor.getDefinitions(bundle, name).stream()).
            distinct().
            map((d) -> new Context(d)).
            collect(Collectors.toList());
        // Now got the persistence unit definitions.
        synchronized (this) {
            bundles.put(bundle, punits);
            if (bundles.size() == 1) {
                tracker.open();
            }
        }
        addRegistration(bundle);
        return punits;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object stored) {
        // No action taken.
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object stored) {
        List<Context> contexts;
        synchronized (this) {
            contexts = bundles.remove(bundle);
            if (bundles.size() == 0) {
                tracker.close();
            }
        }
        if (contexts != null) {
            contexts.forEach((context) -> destroy(context));
        }
    }
    
    @Override
    public synchronized void added(String providerName, PersistenceProvider p) {
        new ArrayList<>(bundles.keySet()).stream().forEach((b) -> addRegistration(b));
    }

    @Override
    public synchronized void removed(String providerName) {
        bundles.entrySet().stream().
            forEach((e) -> e.getValue().stream().
                    filter((c) -> providerName.equals(c.usedProvider)).forEach((c) -> destroy(c)));
    }

    static class Context {
        EntityManagerFactory factory;
        ServiceRegistration<EntityManagerFactory> registration;
        PersistenceUnitDefinition definition;
        String usedProvider;
        Context(PersistenceUnitDefinition def) {
            this.definition = def;
        }
    }
}
