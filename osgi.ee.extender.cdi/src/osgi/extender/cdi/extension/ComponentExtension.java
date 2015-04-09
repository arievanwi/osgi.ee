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

package osgi.extender.cdi.extension;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import osgi.cdi.annotation.ComponentScoped;

/**
 * Extension to immediately create those beans that are put
 * in the component scope.
 */
public class ComponentExtension implements Extension {
    private Collection<Bean<?>> theseBeans = new ArrayList<>();
    
    public <T> void processBean(@Observes ProcessBean<T> event) {
        if (event.getAnnotated().isAnnotationPresent(ComponentScoped.class)) {
            // Add this one.
            theseBeans.add(event.getBean());
        }
    }
 
    public void finish(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
        for (Bean<?> bean : theseBeans) {
            Object obj = beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
            // Since the object returned may be a proxy, force loading of the bean.
            obj.toString();
        }
    }
}
