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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

import org.jboss.weld.resources.spi.ResourceLoader;
import org.osgi.framework.Bundle;

import osgi.extender.cdi.Helper;

/**
 * Weld CDI resource loader implementation that loads resources from a bundle.
 */
class BundleResourceLoader implements ResourceLoader {
    private ClassLoader loader;
    
    BundleResourceLoader(Bundle bundle) {
        this.loader = Helper.getLoader(bundle);
    }
    
    @Override
    public void cleanup() {
    }

    /**
     * Get the class loader for the current bundle context.
     * 
     * @return The class loader for the 
     */
    private ClassLoader loader() {
        return loader;
    }
    
    private Collection<ClassLoader> loaders() {
        return Arrays.asList(loader(), BundleResourceLoader.class.getClassLoader());
    }
    
    @Override
    public Class<?> classForName(String name) {
        return Helper.loadClass(loaders(), name);
    }

    @Override
    public URL getResource(String name) {
        URL url = null;
        for (ClassLoader l : loaders()) {
            url = l.getResource(name);
            if (url != null) break;
        }
        return url;
    }

    @Override
    public Collection<URL> getResources(String name) {
        try {
            Collection<URL> urls = new ArrayList<>();
            for (ClassLoader l : loaders()) {
                Enumeration<URL> found = l.getResources(name);
                while (found.hasMoreElements()) {
                    URL url = found.nextElement();
                    urls.add(url);
                }
            }
            return urls;
        } catch (Exception exc) {
            throw new RuntimeException("failed to load resources \"" + name + "\"", exc);
        }
    }
}
