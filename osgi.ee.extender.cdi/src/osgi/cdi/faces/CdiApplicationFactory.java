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
package osgi.cdi.faces;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;

/**
 * Application factory that takes care of the CDI handling related for JSF. Basically, this
 * consists of wrapping the expression factory and registration of an ELResolver with the existing
 * application. All this functionality is in the {@link CdiApplication} class.
 * 
 * @author Arie van Wijngaarden
 */
public class CdiApplicationFactory extends ApplicationFactory {
    private ApplicationFactory delegate;
    private CdiApplication application;
    
    public CdiApplicationFactory(ApplicationFactory delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public synchronized Application getApplication() {
        if (application == null) {
            application = new CdiApplication(delegate.getApplication());
        }
        return application;
    }

    @Override
    public synchronized void setApplication(Application application) {
        delegate.setApplication(application);
        this.application = null;
    }
}
