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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * Base class for contexts. Takes care of context management, meaning the caching of beans and 
 * instances as needed by the CDI container. It allows for multiple caches per scope, hence enabling
 * the use of request and session scopes. 
 * 
 * @author Arie van Wijngaarden
 */
public abstract class AbstractContext implements AlterableContext {
    private Class<? extends Annotation> scope;
    private Map<Object, ContextBeansHolder> scopeEntry;
    private ContextBeansListener listener;
    
    /**
     * Create a context for the specific scope.
     * 
     * @param scope The scope to create the context for
     * @param listener The listener for beans for which instances are created/deleted
     */
    AbstractContext(Class<? extends Annotation> scope, ContextBeansListener listener) {
        this.scope = scope;
        this.scopeEntry = new HashMap<Object, ContextBeansHolder>();
        this.listener = listener;
    }
    
    @Override
    public <T> T get(Contextual<T> bean) {
        @SuppressWarnings("unchecked")
        T toReturn = (T) getCache().get(bean);
        return toReturn;
    }

    @Override
    public <T> T get(Contextual<T> bean, CreationalContext<T> context) {
        T toReturn = get(bean);
        if (toReturn == null) {
            toReturn = getCache().put(bean, context);
        }
        return toReturn;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public boolean isActive() {
        return scopeEntry.size() > 0;
    }

    @Override
    public void destroy(Contextual<?> bean) {
        getCache().remove(bean);
    }

    /**
     * Add a new variant of this scope's cache. The variant is indexed/identified
     * by the object passed which should further be used to find the related cache.
     * 
     * @param managed The object that serves as cache key entry
     */
    public void add(Object managed) {
        scopeEntry.put(managed, new ContextBeansHolder(listener));
    }
    
    /**
     * Remove the cache for the specific identifier and clean it up.
     * 
     * @param managed The key/identifier of the cache
     */
    public void remove(Object managed) {
        ContextBeansHolder cache = scopeEntry.remove(managed);
        if (cache != null)
            cache.destroy();
    }
    
    /**
     * Clean up the mess.
     */
    public void destroy() {
        scopeEntry.values().stream().forEach((c) -> c.destroy());
    }
    
    /**
     * Get a cache from the set with managed caches. 
     * 
     * @param managed The identifier of the cache. Should be used earlier
     * in {@link #add(Object)} to create the cache.
     * @return A bean cache, or null if the add method was not called earlier
     */
    protected ContextBeansHolder getCache(Object managed) {
        return scopeEntry.get(managed);
    }
    
    /**
     * Get the cache that is currently active. The way "active" is defined is dependent on 
     * the actual subclass implementation.
     * 
     * @return The bean cache found
     */
    protected abstract ContextBeansHolder getCache();
}
