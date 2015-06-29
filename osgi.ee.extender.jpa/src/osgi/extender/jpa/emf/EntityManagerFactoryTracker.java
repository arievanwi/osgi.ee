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
package osgi.extender.jpa.emf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * Tracker for entity manager factories. Factories result automatically in an
 * entity manager that is a thread-local proxy to a real entity manager. That
 * entity manager is completely managed via a transaction manager and therefore
 * the application has no involvement anymore in the handling of transactions.
 * 
 * @author Arie van Wijngaarden
 */
@Component
public class EntityManagerFactoryTracker {
    private static String UNITNAME = EntityManagerFactoryBuilder.JPA_UNIT_NAME;
    private Map<String, EntityManagerFactory> factories = new HashMap<>();
    private Map<String, ServiceRegistration<EntityManager>> entityManagers = new HashMap<>();
    private TransactionManager transactionManager;
    private Class<?> proxy;
    private BundleContext context;

    public EntityManagerFactoryTracker() {
        proxy = Proxy.getProxyClass(getClass().getClassLoader(),
                new Class<?>[] {EntityManager.class});
    }

    private static String unitName(Map<String, Object> props) {
        return (String) props.get(UNITNAME);
    }

    @Activate
    synchronized void activate(BundleContext cont) {
        this.context = cont;
        factories.entrySet().forEach((e) -> register(e.getKey(), e.getValue()));
    }

    /**
     * Register an entity manager for a factory. Is called during the dynamics
     * (either at start or afterwards) to handle the creation of an entity
     * manager.
     * 
     * @param unitName The unit to register for
     * @param factory The factory used
     */
    private synchronized void register(String unitName,
            EntityManagerFactory factory) {
        if (context == null || transactionManager == null)
            return;
        if (entityManagers.containsKey(unitName)) {
            throw new RuntimeException("(bugcheck): registration for unit " +
                unitName + " already done");
        }
        // Proxy an entity manager.
        EntityManager manager = proxy(factory);
        // Register the proxy as service.
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(UNITNAME, unitName);
        ServiceRegistration<EntityManager> sr = context.registerService(
                EntityManager.class, manager, props);
        entityManagers.put(unitName, sr);
    }

    private synchronized void unregister(String unitName) {
        ServiceRegistration<EntityManager> manager = entityManagers.remove(unitName);
        if (manager != null)
            try {
                manager.unregister();
            } catch (Exception exc) {
            }
    }

    /**
     * Entity manager factory setter. Takes care of setting the entity manager
     * factory to the internal list.
     * 
     * @param factory The factory registered
     * @param properties The properties belonging to the service
     */
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    synchronized void addEntityManagerFactory(EntityManagerFactory factory,
            Map<String, Object> properties) {
        // Just in case we are replaced.
        removeEntityManagerFactory(null, properties);
        // Get the unit name and put the factory in a local map. We could do
        // without, but maybe it is handy for future extensions/changes.
        String unitName = unitName(properties);
        factories.put(unitName, factory);
        register(unitName, factory);
    }

    synchronized void removeEntityManagerFactory(EntityManagerFactory factory,
            Map<String, Object> properties) {
        // Remove the entity manager.
        String unitName = unitName(properties);
        EntityManagerFactory f = factories.get(unitName);
        if (factory == null || factory.equals(f)) {
            factories.remove(unitName);
            unregister(unitName);
        }
    }

    @Reference
    void setTransactionManager(TransactionManager manager) {
        this.transactionManager = manager;
    }

    @Deactivate
    synchronized void deactivate() {
        entityManagers.values().stream().forEach((sr) -> sr.unregister());
    }

    /**
     * This method creates a proxy for an entity manager.
     * 
     * @param factory The entity manager factory to create the proxy for
     * @return The entity manager proxy
     */
    private EntityManager proxy(EntityManagerFactory factory) {
        InvocationHandler handler = new EntityProxyInvocationHandler(factory,
                transactionManager);
        try {
            EntityManager manager = (EntityManager) proxy.getConstructor(
                    InvocationHandler.class).newInstance(handler);
            return manager;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
}
