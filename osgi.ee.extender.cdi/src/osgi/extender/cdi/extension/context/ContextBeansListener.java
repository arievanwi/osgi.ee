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

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * Listener for context beans. Is notified when beans instances are removed or added to
 * the bean instance holder.
 */
public interface ContextBeansListener {
    /**
     * Notification that a new instance of a bean was added to the bean holder.
     * 
     * @param bean The bean for which the action took place
     * @param context The creational context that was used
     * @param instance The instance added
     */
    public void instanceAdded(Contextual<?> bean, CreationalContext<?> context, Object instance);
    /**
     * Notification that a bean was removed from the bean holder.
     * 
     * @param bean The bean
     * @param context The creational context
     * @param instance The instance
     */
    public void instanceRemoved(Contextual<?> bean, CreationalContext<?> context, Object instance);
}
