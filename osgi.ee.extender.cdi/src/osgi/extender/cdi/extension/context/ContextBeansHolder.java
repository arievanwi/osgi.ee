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
package osgi.extender.cdi.extension.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * Cache of beans. Actually a map that contains instances of beans for a specific context. It is used
 * by the context to maintain the caching for a specific scope variant.
 */
class ContextBeansHolder {
    public static final ContextBeansHolder EMPTY = new ContextBeansHolder(null);
    private Map<Contextual<?>, CacheEntry> cache = new HashMap<>();
    private ContextBeansListener listener;
    
    /**
     * Create the holder/cache of the beans.
     * 
     * @param listener A listener for changes to the cache
     */
    ContextBeansHolder(ContextBeansListener listener) {
        this.listener = listener;
        if (this.listener == null) {
            this.listener = new ContextBeansListener() {
                @Override
                public void instanceRemoved(Contextual<?> bean,
                        CreationalContext<?> context, Object instance) {
                }
                @Override
                public void instanceAdded(Contextual<?> bean, CreationalContext<?> context,
                        Object instance) {
                }
            };
        }
    }
    
    /**
     * Get the object from the cache for a bean.
     * 
     * @param bean The bean to get the object for
     * @return The object, or null if not present in the cache
     */
    Object get(Contextual<?> bean) {
        CacheEntry entry = cache.get(bean);
        if (entry == null) return null;
        return entry.instance;
    }
    
    /**
     * Put a bean into the cache.
     * 
     * @param bean The bean to instantiate
     * @param context The creational context used to instantiate the bean
     * @return The instantiated bean instance
     */
    @SuppressWarnings("unchecked")
    <T> T put(Contextual<T> bean, CreationalContext<T> context) {
        T obj = bean.create(context);
        synchronized (cache) {
            cache.put(bean, new CacheEntry(obj, (CreationalContext<Object>) context));
        }
        listener.instanceAdded(bean, context, obj);
        return obj;
    }
    
    /**
     * Remove a bean from the cache and call the listeners for removal
     * 
     * @param bean The bean to remove
     */
    void remove(Contextual<?> bean) {
        @SuppressWarnings("unchecked")
        Contextual<Object> thisOne = (Contextual<Object>) bean;
        CacheEntry entry;
        synchronized (cache) {
            entry = cache.remove(bean);
        }
        if (entry != null) {
            listener.instanceRemoved(bean, entry.context, entry.instance);
            thisOne.destroy(entry.instance, entry.context);
        }
    }
    
    /**
     * Destroy this bean cache.
     */
    void destroy() {
        new ArrayList<>(cache.keySet()).stream().forEach((b) -> remove(b));
    }
    
    private class CacheEntry {
        final Object instance;
        final CreationalContext<Object> context;
        CacheEntry(Object instance, CreationalContext<Object> context) {
            this.instance = instance;
            this.context = context;
        }
    }
}
