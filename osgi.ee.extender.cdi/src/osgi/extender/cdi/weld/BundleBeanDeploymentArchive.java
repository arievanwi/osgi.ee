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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWiring;

import osgi.extender.cdi.Helper;

/**
 * Weld bean deployment archive for beans from a bundle. Is Weld specific, so see the Weld bootstrap
 * documentation about how a deployment archive is used. 
 */
class BundleBeanDeploymentArchive implements BeanDeploymentArchive {
    private Bundle bundle;
    private BeansXml beanFiles;
    private SimpleServiceRegistry services = new SimpleServiceRegistry();

    public BundleBeanDeploymentArchive(Bundle b, BeansXml beanFiles) {
        this.bundle = b;
        services.add(ResourceLoader.class, new BundleResourceLoader(bundle));
        this.beanFiles = (beanFiles == null) ? BeansXml.EMPTY_BEANS_XML : beanFiles;
    }
    
    /**
     * Retrieve the bean classes that need to be processed by the CDI container. We only
     * include the classes on our own class path, nothing in jars, although this is
     * basically a requirement from CDI. However, we think this is bad practice.
     */
    @Override
    public Collection<String> getBeanClasses() {
        // Get the bundle class path.
        String cp = bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
        if (cp == null) {
            cp = ".";
        }
        Stream<String> paths = Arrays.asList(cp.split(",")).stream();
        // Trick to make sure that running/debugging on eclipse doesn't mess things up.
        try (InputStream in = Helper.getLoader(bundle).getResourceAsStream("/build.properties")) {
            Properties props = new Properties();
            props.load(in);
            String replacement = props.getProperty("output..");
            if (replacement != null) {
                paths = paths.map((s) -> (".".equals(s)) ? replacement : s);
            }
        } catch (Exception exc) {}
        return paths.map((a) -> getBeans(a)).flatMap((a) -> a.stream()).collect(Collectors.toList());
    }

    private Collection<String> getBeans(final String path) {
        BundleWiring wiring = Helper.getWiring(bundle);
        String p = path.trim().replaceAll("\\.", "");
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        int length = p.length();
        Collection<String> toReturn = wiring.findEntries(p, "*.class", BundleWiring.FINDENTRIES_RECURSE).stream().map(
                (u) -> u.getFile().replace(".class",  "").replaceAll("/",  ".").substring(length)).collect(Collectors.toList());
        return toReturn;
    }
    
    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return new ArrayList<>();
    }

    @Override
    public BeansXml getBeansXml() {
        return beanFiles;
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        return new ArrayList<>();
    }

    @Override
    public String getId() {
        return "cdi:" + bundle.getBundleId();
    }

    @Override
    public ServiceRegistry getServices() {
        return services;
    }

    @Override
    public String toString() {
        return getId();
    }
}
