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
import javax.transaction.Status;
import javax.transaction.Synchronization;

/**
 * Synchronizer for JTA transaction based functionality.
 */
class JTASynchronization implements Synchronization {
    private ThreadLocal<EntityManager> local;
    private boolean flushOnCommit;
    private boolean evictCache;

    public JTASynchronization(ThreadLocal<EntityManager> l, boolean flushOnCommit, boolean evictCache) {
        local = l;
        this.flushOnCommit = flushOnCommit;
        this.evictCache = evictCache;
    }

    @Override
    public void afterCompletion(int status) {
        EntityManager manager = local.get();
        if (manager != null) {
            try {
                if (flushOnCommit && (status == Status.STATUS_COMMITTED || status == Status.STATUS_COMMITTING)) {
                    // Flushing requested.
                    try {
                        manager.flush();
                    } catch (Exception exc) {}
                }
                if (evictCache) {
                    // Wants to evict the cache as well.
                    manager.getEntityManagerFactory().getCache().evictAll();
                }
                manager.close();
            } finally {
                local.remove();
            }
        }
    }

    @Override
    public void beforeCompletion() {
    }
}
