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
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
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
    private EntityManagerFactory factory;
    private ThreadLocal<EntityManager> local;
    private TransactionManager transactionManager;
    
    /**
     * Create an proxy invocation handler for a factory and a transaction manager.
     * 
     * @param f The entity manager factory
     * @param m The transaction manager
     */
    EntityProxyInvocationHandler(EntityManagerFactory f, TransactionManager m) {
        this.factory = f;
        local = new ThreadLocal<>();
        this.transactionManager = m;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        EntityManager manager = local.get();
        if (manager == null) {
            manager = factory.createEntityManager();
            registerEntityManager(manager, local, transactionManager);
            local.set(manager);
        }
        if (method.getName().equals("getTransaction")) {
            throw new RuntimeException("(bugcheck): transactions are automatically managed");
        }
        return method.invoke(manager, args);
    }
    
    /**
     * Register an entity manager with a transaction manager.
     * 
     * @param manager The entity manager to register, which is a non-proxy one
     * @param local The thread local that maintains the entity managers for the various threads
     * @param transactionManager The transaction manager to register with
     */
    private static void registerEntityManager(EntityManager manager, final ThreadLocal<EntityManager> local, 
            final TransactionManager transactionManager) throws Exception {
        Synchronization sync;
        try {
            EntityTransaction trans = manager.getTransaction();
            trans.begin();
            manager.joinTransaction();
            sync = new ResourceLocalSynchronization(trans, local);
        } catch (IllegalStateException exc) {
            sync = new JTASynchronization(local);
        }
        transactionManager.getTransaction().registerSynchronization(sync);
    }
}
