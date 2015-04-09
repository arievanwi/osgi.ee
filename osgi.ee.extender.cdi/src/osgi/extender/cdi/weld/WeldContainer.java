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
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import osgi.extender.cdi.extension.ComponentExtension;
import osgi.extender.cdi.extension.ScopeExtension;
import osgi.extender.cdi.extension.ServiceExtension;
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
    
    WeldContainer(Bundle toExtend) {
        boot = new WeldBootstrap();
        String contextName = "osgi-cdi:" + toExtend.getBundleId();
        // Construct our extension which does the main of the work.
        BundleContext context = toExtend.getBundleContext();
        List<Extension> extensions = Arrays.asList(
                new ScopeExtension(context),
                new ServiceExtension(context),
                new ComponentExtension());
        deployment = new OurDeployment(toExtend,
                extensions.stream().map((e) -> new OurMetaData<>(e.getClass().getName(), e)).collect(Collectors.toList()));
        // Start the container. Needs to be called now because otherwise no bean manager is returned from the bootstrap.
        boot.startContainer(contextName, new OurEnvironment(), deployment);
        // Takes care of the extension initialization. We would want to do this inside the separate
        // thread, but this causes some exception breakpoints because our extensions are not proxyable.
        boot.startInitialization();
        // The remainder is done in a separate thread.
        Runnable runner = () -> {
            // Go through the bootstrap sequence. This automatically fires the
            // events to our extensions.
            boot.deployBeans();
            boot.validateBeans();
            boot.endInitialization();
        };
        new Thread(runner).start();
    }
    
    BeanManager getManager() {
        return boot.getManager(deployment.getBeanDeploymentArchives().iterator().next());
    }
    
    void destroy() {
        boot.shutdown();
    }
}
