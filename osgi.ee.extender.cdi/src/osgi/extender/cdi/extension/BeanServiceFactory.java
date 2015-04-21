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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Service factory that wraps a bean so an instance is returned for the bean
 * on every service get request and released on every service release request. This 
 * is especially important where the creation of services is preferably done lazy, for
 * example during start-up of a CDI application.
 */
class BeanServiceFactory<T> implements ServiceFactory<T> {
    private Bean<T> bean;
    private CreationalContext<T> cc;
    
    BeanServiceFactory(CreationalContext<T> cc, Bean<T> bean) {
        this.bean = bean;
        this.cc = cc;
    }
    
    @Override
    public T getService(Bundle bundle, ServiceRegistration<T> registration) {
        T instance = bean.create(cc);
        return instance;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<T> reg, T object) {
        bean.destroy(object, cc);
    }
}