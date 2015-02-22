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

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.osgi.framework.Bundle;

import osgi.extender.cdi.extension.OurExtension;
import osgi.extender.cdi.extension.OurMetaData;

/**
 * Creator for a Weld container related to a specific bundle that is extended. Basically,
 * the holder of one extended bundle.
 * 
 * @author Arie van Wijngaarden
 */
class WeldContainer {
    private WeldBootstrap boot;
    private Deployment deployment;
    private OurExtension extension;
    
    WeldContainer(Bundle toExtend) {
        boot = new WeldBootstrap();
        String contextName = "osgi-cdi:" + toExtend.getBundleId();
        // Construct our extension which does the main of the work.
        extension = new OurExtension(toExtend.getBundleContext());
        deployment = new OurDeployment(toExtend, Arrays.asList(
                new OurMetaData<Extension>(extension.getClass().getName(), extension)
                ));
        // Go through the bootstrap sequence. This automatically fires the
        // events to our extension.
        boot.startContainer(contextName, new OurEnvironment(), deployment);
        boot.startInitialization();
        boot.deployBeans();
        boot.validateBeans();
        boot.endInitialization();
        // Now the CDI container is up. Finish the work on the extension.
        extension.finish(getManager());
    }
    
    BeanManager getManager() {
        return boot.getManager(deployment.getBeanDeploymentArchives().iterator().next());
    }
    
    void destroy() {
        boot.shutdown();
        extension.destroy();
    }
}
