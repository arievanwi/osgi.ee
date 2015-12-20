/*
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
package osgi.extender.helpers;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Service loader class loader: class loader that constructs a class loader from all
 * class loaders related to a bundle to find resources and classes that may otherwise
 * go unnoticed.
 */
public class DelegatingClassLoader extends ClassLoader {
    private ClassLoader delegate;

    private DelegatingClassLoader(ClassLoader parent, ClassLoader delegate) {
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
     * Get a delegating class loader from a collection of other class loaders. The number of loaders
     * is unimportant.
     *
     * @param classLoaders The class loaders to delegate to
     * @return A class loader that is the combination of them all
     */
    public static ClassLoader from(Collection<ClassLoader> classLoaders) {
        if (classLoaders.size() == 0) {
            return null;
        }
        if (classLoaders.size() == 1) {
            return classLoaders.iterator().next();
        }
        Iterator<ClassLoader> it = classLoaders.iterator();
        DelegatingClassLoader loader = new DelegatingClassLoader(it.next(), it.next());
        while (it.hasNext()) {
            loader = new DelegatingClassLoader(loader, it.next());
        }
        return loader;
    }

    /**
     * Create a class loader from a bundle with all its dependencies.
     *
     * @param bundle The bundle to create the class loader for
     * @return The class loader
     */
    public static ClassLoader from(Bundle bundle) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        List<ClassLoader> loaders = new ArrayList<>();
        loaders.add(wiring.getClassLoader());
        wiring.getRequiredWires(null).stream().
                map((w) -> w.getProvider().getBundle().adapt(BundleWiring.class).getClassLoader()).distinct().
                forEach((l) -> loaders.add(l));
        return from(loaders);
    }
}
