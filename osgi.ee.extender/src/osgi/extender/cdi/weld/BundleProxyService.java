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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;

import osgi.extender.helpers.CompoundClassLoader;

/**
 * Proxy service handling for proxying beans. Basically, proxying is done using the combination
 * of the class loader from the bundle we are extending combined with the class loaders of the weld bundle(s).
 */
class BundleProxyService implements ProxyServices {
    private Collection<ClassLoader> delegates;
    
    BundleProxyService(Bundle extendedBundle) {
        delegates = new ArrayList<>();
        ClassLoader extendedBundleLoader = Helper.getWiring(extendedBundle).getClassLoader();
        // Locate the correct bundle and get the class loader from it.
        // Note that this is kind of dirty since we check on the symbolic name.
        Collection<ClassLoader> found = Arrays.asList(extendedBundle.getBundleContext().getBundles()).stream().
                filter((a) -> a.getSymbolicName().contains("weld")).
                map((a) -> Helper.getLoader(a)).collect(Collectors.toList());
        delegates.addAll(found);
        delegates.add(extendedBundleLoader);
    }
    
    @Override
    public void cleanup() {
    }

    @Override
    public ClassLoader getClassLoader(Class<?> beanType) {
        return CompoundClassLoader.from(delegates.toArray(new ClassLoader[delegates.size()]));
    }

    @Override
    @Deprecated
    public Class<?> loadBeanClass(String name) {
        throw new RuntimeException("(bugcheck): deprecated");
    }
}
