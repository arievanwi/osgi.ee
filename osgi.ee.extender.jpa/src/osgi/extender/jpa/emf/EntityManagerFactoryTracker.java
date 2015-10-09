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
import javax.persistence.spi.PersistenceUnitTransactionType;
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
    private Map<EntityManagerFactory, Map<String, Object>> factories = new HashMap<>();
    private Map<EntityManagerFactory, ServiceRegistration<EntityManager>> entityManagers = new HashMap<>();
    private TransactionManager transactionManager;
    private Class<?> proxy;
    private BundleContext context;

    public EntityManagerFactoryTracker() {
        proxy = Proxy.getProxyClass(getClass().getClassLoader(),
                new Class<?>[] {EntityManager.class});
    }

    /**
     * Get the unit name property from the service properties.
     *
     * @param props The service properties
     * @return The unit name
     */
    private static String unitName(Map<String, Object> props) {
        return (String) props.get(UNITNAME);
    }

    /**
     * Get the persistence unit transaction type from the service properties.
     *
     * @param props The properties
     * @return The transaction type
     */
    private static PersistenceUnitTransactionType transactionType(Map<String, Object> props) {
        Object value = props.get(PersistenceUnitTransactionType.class.getName());
        if (value == null) {
            return null;
        }
        return PersistenceUnitTransactionType.valueOf(value.toString());
    }

    @Activate
    synchronized void activate(BundleContext cont) {
        context = cont;
        factories.entrySet().forEach((e) -> register(e.getKey(), e.getValue()));
    }

    /**
     * Register an entity manager for a factory. Is called during the dynamics
     * (either at start or afterwards) to handle the creation of an entity
     * manager.
     *
     * @param factory The factory used
     * @param
     */
    private synchronized void register(EntityManagerFactory factory, Map<String, Object> properties) {
        if (context == null || transactionManager == null || entityManagers.containsKey(factory)) {
            return;
        }
        String unitName = unitName(properties);
        PersistenceUnitTransactionType type = transactionType(properties);
        // Proxy an entity manager.
        EntityManager manager = proxy(factory, type);
        // Register the proxy as service.
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(UNITNAME, unitName);
        ServiceRegistration<EntityManager> sr = context.registerService(
                EntityManager.class, manager, props);
        entityManagers.put(factory, sr);
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
        factories.put(factory, properties);
        register(factory, properties);
    }

    synchronized void removeEntityManagerFactory(EntityManagerFactory factory) {
        // Remove the entity manager.
        factories.remove(factory);
        // Unregister the service.
        ServiceRegistration<EntityManager> manager = entityManagers.remove(factory);
        if (manager != null) {
            try {
                manager.unregister();
            } catch (Exception exc) {
            }
        }
    }

    @Reference
    void setTransactionManager(TransactionManager manager) {
        transactionManager = manager;
    }

    @Deactivate
    synchronized void deactivate() {
        entityManagers.values().stream().forEach((sr) -> sr.unregister());
    }

    /**
     * This method creates a proxy for an entity manager.
     *
     * @param factory The entity manager factory to create the proxy for
     * @param type The transaction type for this unit
     * @return The entity manager proxy
     */
    private EntityManager proxy(EntityManagerFactory factory, PersistenceUnitTransactionType type) {
        InvocationHandler handler = new EntityProxyInvocationHandler(factory,
                transactionManager, type);
        try {
            EntityManager manager = (EntityManager) proxy.getConstructor(
                    InvocationHandler.class).newInstance(handler);
            return manager;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
}
