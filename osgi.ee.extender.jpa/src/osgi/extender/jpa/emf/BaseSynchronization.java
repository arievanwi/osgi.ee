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

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;

/**
 * Synchronizer for basic transaction handling.
 */
abstract class BaseSynchronization implements Synchronization {
    private ThreadLocal<EntityManager> local;

    public BaseSynchronization(ThreadLocal<EntityManager> l) {
        local = l;
    }

    protected void clear() {
        local.remove();
    }

    protected abstract void doAfterCompletion(int status);

    @Override
    public final void afterCompletion(int status) {
        try {
            EntityManager em = local.get();
            if (em != null) {
                em.close();
            }
            doAfterCompletion(status);
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            local.remove();
        }
    }

    /**
     * The before completion method is not called in case a transaction is
     * rolled-back. As such, no actions should be done there.
     */
    @Override
    public final void beforeCompletion() {
    }
}