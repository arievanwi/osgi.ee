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
package osgi.extender.jpa.service;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Class loader that delegates to other class loaders in order of appearance.
 */
class CompoundClassLoader extends ClassLoader {
    private ClassLoader delegate;
    
    private CompoundClassLoader(ClassLoader parent, ClassLoader delegate) {
        super(parent);
        this.delegate = delegate;
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return delegate.loadClass(name);
    }

    @Override
    protected URL findResource(String name) {
        return delegate.getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return delegate.getResources(name);
    }
    
    /**
     * Get a compound class loader from a set of other loaders. The number of loaders
     * is unimportant.
     * 
     * @param classLoaders The class loaders to delegate to
     * @return A class loader that is the combination of them all
     */
    public static ClassLoader from(ClassLoader... classLoaders) {
        if (classLoaders.length == 0) return null;
        if (classLoaders.length == 1) return classLoaders[0];
        CompoundClassLoader loader = new CompoundClassLoader(classLoaders[0], classLoaders[1]);
        for (int cnt = 2; cnt < classLoaders.length; cnt++) {
            loader = new CompoundClassLoader(loader, classLoaders[cnt]);
        }
        return loader;
    }
}
