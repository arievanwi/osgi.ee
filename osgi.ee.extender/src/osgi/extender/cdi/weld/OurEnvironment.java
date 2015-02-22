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
package osgi.extender.cdi.weld;

import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * Environment specification for Weld that matches the extender context here. Rather
 * useless, since we add the services anyway. No checking is needed by Weld on this.
 */
class OurEnvironment implements Environment {

    @Override
    public Set<Class<? extends Service>> getRequiredBeanDeploymentArchiveServices() {
        Set<Class<? extends Service>> services = new HashSet<>();
        services.add(ResourceLoader.class);
        return services;
    }

    @Override
    public Set<Class<? extends Service>> getRequiredDeploymentServices() {
        Set<Class<? extends Service>> services = new HashSet<>();
        return services;
    }
}
