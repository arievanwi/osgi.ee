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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TransactionRequiredException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

/**
 * Invocation handler for an entity manager proxy. Since entity managers are not thread safe, this
 * handler takes care of providing every thread with its own instance. The instance is registered with
 * the transaction manager to make sure that transactions are actually handled correctly and the
 * entity manager is flushed, etc.
 *
 * @author Arie van Wijngaarden
 */
class EntityProxyInvocationHandler implements InvocationHandler {
    @FunctionalInterface
    interface RegisterSynchronization {
        void registerEntityManager(EntityManager manager, final ThreadLocal<EntityManager> local,
                final TransactionManager transactionManager) throws Exception;
    }
    private EntityManagerFactory factory;
    private ThreadLocal<EntityManager> local;
    private TransactionManager transactionManager;
    private RegisterSynchronization synchronizer;

    /**
     * Create an proxy invocation handler for a factory and a transaction manager.
     *
     * @param f The entity manager factory
     * @param m The transaction manager
     * @param type The persistence unit transaction type
     */
    EntityProxyInvocationHandler(EntityManagerFactory f, TransactionManager m, PersistenceUnitTransactionType type) {
        factory = f;
        local = new ThreadLocal<>();
        transactionManager = m;
        if (PersistenceUnitTransactionType.JTA.equals(type)) {
            synchronizer = (a, b, c) -> registerEntityManagerJTA(a, b, c);
        }
        else if (PersistenceUnitTransactionType.RESOURCE_LOCAL.equals(type)) {
            synchronizer = (a, b, c) -> registerEntityManagerResourceLocal(a, b, c);
        }
        else {
            synchronizer = (a, b, c) -> registerEntityManagerUnknown(a, b, c);
        }
    }

    /**
     * Handle some well known methods that are known to cause race conditions during framework
     * shutdown and startup.
     *
     * @param proxy The proxy
     * @param method The method invoked on the proxy
     * @param args The arguments to the method invocation
     * @return The return of the action, if handled. Otherwise null
     */
    private Object handleWellKnownMethods(Object proxy, Method method, Object[] args) {
        if ("hashCode".equals(method.getName())) {
            return factory.hashCode();
        }
        if ("equals".equals(method.getName())) {
            Object firstArg = args[0];
            if (proxy == firstArg) {
                return true;
            }
            return false;
        }
        if ("toString".equals(method.getName())) {
            return "Proxy for EntityManager of " + factory;
        }
        return null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("getTransaction")) {
            throw new RuntimeException("(bugcheck): transactions are automatically managed");
        }
        Object toReturn = handleWellKnownMethods(proxy, method, args);
        if (toReturn != null) {
            return toReturn;
        }
        EntityManager manager = local.get();
        if (manager == null) {
            manager = factory.createEntityManager();
            synchronizer.registerEntityManager(manager, local, transactionManager);
            local.set(manager);
        }
        try {
            return method.invoke(manager, args);
        } catch (InvocationTargetException exc) {
            throw exc.getCause();
        }
    }

    /**
     * Register an entity manager with a transaction manager, JTA based.
     *
     * @param manager The entity manager to register, which is a non-proxy one
     * @param local The thread local that maintains the entity managers for the various threads
     * @param transactionManager The transaction manager to register with
     */
    private static void registerEntityManagerJTA(EntityManager manager, final ThreadLocal<EntityManager> local,
            final TransactionManager transactionManager) throws Exception {
        Synchronization sync = new JTASynchronization(local, false, false);
        transactionManager.getTransaction().registerSynchronization(sync);
        manager.joinTransaction();
    }

    /**
     * Register an entity manager with a transaction manager, resource local based.
     *
     * @param manager The entity manager to register, which is a non-proxy one
     * @param local The thread local that maintains the entity managers for the various threads
     * @param transactionManager The transaction manager to register with
     */
    private static void registerEntityManagerResourceLocal(EntityManager manager, final ThreadLocal<EntityManager> local,
            final TransactionManager transactionManager) throws Exception {
        EntityTransaction trans = manager.getTransaction();
        trans.begin();
        transactionManager.getTransaction().registerSynchronization(new ResourceLocalSynchronization(trans, local));
    }

    /**
     * Register an entity manager with a transaction manager.
     *
     * @param manager The entity manager to register, which is a non-proxy one
     * @param local The thread local that maintains the entity managers for the various threads
     * @param transactionManager The transaction manager to register with
     */
    private static void registerEntityManagerUnknown(EntityManager manager, final ThreadLocal<EntityManager> local,
            final TransactionManager transactionManager) throws Exception {
        try {
            manager.joinTransaction();
            Synchronization sync = new JTASynchronization(local, true, true);
            transactionManager.getTransaction().registerSynchronization(sync);
        } catch (TransactionRequiredException exc) {
            registerEntityManagerResourceLocal(manager, local, transactionManager);
        }
    }
}
