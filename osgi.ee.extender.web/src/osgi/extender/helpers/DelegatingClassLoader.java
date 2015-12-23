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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Service loader class loader: class loader that constructs a class loader from all
 * class loaders related to a bundle to find resources and classes that may otherwise
 * go unnoticed.
 *
 * Note that this class loader is as copy available in the CDI extender bundle. Any changes here must
 * reflect there as well (they are not shared).
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
        return from(getDependencies(bundle).stream().
                map((b) -> b.adapt(BundleWiring.class).getClassLoader()).
                collect(Collectors.toList()));
    }

    /**
     * Get the 1st line dependencies (and the bundle itself) for a bundle.
     *
     * @param bundle The bundle to get the dependencies for
     * @return A collection with the bundle and 1st line dependencies
     */
    public static Collection<Bundle> getDependencies(Bundle bundle) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        Set<Bundle> out = new HashSet<>();
        out.add(bundle);
        wiring.getRequiredWires(null).stream().
            map((w) -> w.getProvider().getBundle()).forEach((b) -> out.add(b));
        return out;
    }
}