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

import javax.enterprise.context.spi.Contextual;

import osgi.extender.cdi.scopes.ExtenderContext;
import osgi.extender.cdi.scopes.ScopeListener;

/**
 * Implementation of a context that is able to handle a scope with the option to have multiple caches
 * per scope. The cache that is used is determined using the {@link #setCurrent(Object)} method that
 * activates a specific cache for a thread. Therefore, it is impossible to have multiple caches for
 * the same scope within one thread. The normal usage scenario is as follows:
 * <ol>
 * <li>The context is created by our own extension that is connected to the bean manager for a bundle during
 * the "after bean discovery" phase.</li>
 * <li>The bean manager creates a service for it to allow manipulation via the {@link ExtenderContext} interface
 * (for example in the request or session life cycle).</li>
 * <li>This is normally done using the {@link ScopeListener} servlet listener that creates new caches based
 * on the creation of requests or sessions. Therefore, web applications that use the contexts should declare
 * this listener in their web.xml file.</li>
 * </ol>
 * Note that, although the context is bundle specific, it can (and will) be manipulated globally via
 * the service. This makes it for example possible to synchronize sessions over all bundles in one go.
 * 
 * @author Arie van Wijngaarden
 */
public class MultiInstanceContext extends AbstractContext implements ExtenderContext {
    private ThreadLocal<Object> current;
    
    /**
     * Create a multi-instance context with a specific scope.
     * 
     * @param scope The scope for which to create the context
     * @param listener The listener for changes to the caches
     */
    public MultiInstanceContext(Class<? extends Annotation> scope, ContextBeansListener listener) {
        super(scope, listener);
        this.current = new ThreadLocal<>();
    }
    
    @Override
    public void setCurrent(Object managed) {
        if (managed == null) {
            this.current.remove();
        }
        else {
            this.current.set(managed);
            if (getCache(managed) == null) {
                add(managed);
            }
        }
    }
    
    @Override
    protected ContextBeansHolder getCache() {
        ContextBeansHolder thisOne = getCache(this.current.get());
        if (thisOne == null) {
            throw new RuntimeException("(bugcheck): cache not found for current context");
        }
        return thisOne;
    }
    
    @Override
    public void destroy(Contextual<?> bean) {
        getCache().remove(bean);
    }
}
