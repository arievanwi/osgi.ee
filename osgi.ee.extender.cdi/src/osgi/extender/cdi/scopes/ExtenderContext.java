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
package osgi.extender.cdi.scopes;

import java.lang.annotation.Annotation;

/**
 * The interface to a CDI context within the CDI extender that should be used by bundles to manipulate 
 * the setting of the current thread scope context as is defined by the CDI specification (one thread can
 * only have one scope). Normally, this would be used by servlet listeners to set the session scopes
 * and request scopes into the container before anything useful is done within the container using these
 * scopes.<br/> 
 * In the extender context, the current context is identified by an object. Therefore as long as the object
 * passed is the same, contexts are handled as such.
 * 
 * @author Arie van Wijngaarden
 */
public interface ExtenderContext {
    /**
     * Set the thread current context to the specified identifier. Note that this method assumes that
     * the context for this identifier already exists or at least will be added before any action is
     * done for the related scope.
     * 
     * @param identifier The identifier to set as active context for this thread or null to remove it
     */
    public void setCurrent(Object identifier);
    /**
     * Add a context for the specific identifier.
     * 
     * @param identifier The identifier
     */
    public void add(Object identifier);
    /**
     * Remove the context for the specific identifier, since it will not be used after this
     * 
     * @param identifier The identifier 
     */
    public void remove(Object identifier);
    /**
     * Get the scope of this context.
     * 
     * @return The scope of the context
     */
    public Class<? extends Annotation> getScope();
}