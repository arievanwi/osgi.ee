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

import java.util.Arrays;
import java.util.Collection;

import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;

/**
 * Weld deployment that takes care of our specific handling. Adds services that are
 * aware of the OSGi environment from this extender and adds one bean archive with
 * all the beans from this bundle.
 * 
 * @author Arie van Wijngaarden
 */
class OurDeployment implements Deployment {
    private BeanDeploymentArchive archive;
    private ServiceRegistry services;
    private Iterable<Metadata<Extension>> extensions;
    
    /**
     * Construct this deployment.
     * 
     * @param toExtend The bundle we are extending
     * @param beansXml The beans file definition
     * @param extensions The extensions to return
     */
    OurDeployment(Bundle toExtend, BeansXml beansXml, Iterable<Metadata<Extension>> extensions) {
        this.extensions = extensions;
        services = new SimpleServiceRegistry();
        services.add(ProxyServices.class, new BundleProxyService(toExtend));
        archive = new BundleBeanDeploymentArchive(toExtend, beansXml);
    }
    
    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return Arrays.asList(archive);
    }

    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        return extensions;
    }

    @Override
    public ServiceRegistry getServices() {
        return services;
    }

    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> clz) {
        return archive;
    }
}