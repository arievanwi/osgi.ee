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

import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Some helper methods.
 */
class Helper {
    static Class<?> loadClass(Collection<ClassLoader> loaders, String name) {
        Exception lastException = null;
        for (ClassLoader l : loaders) {
            try {
                Class<?> clz = l.loadClass(name);
                return clz;
            } catch (ClassNotFoundException exc) {
                // This is normal.
                if (lastException == null) {
                    lastException = exc;
                }
            } catch (Exception exc) {
                // This is exceptional, so track it.
                lastException = exc;
            }
        }
        throw new RuntimeException("cannot load class \"" + name + "\"", lastException);
    }
    
    static BundleWiring getWiring(Bundle bundle) {
        BundleWiring wire = bundle.adapt(BundleWiring.class);
        if (wire == null) {
            throw new RuntimeException("(bugcheck): no wiring returned for " + bundle);
        }
        return wire;
    }
    
    static ClassLoader getLoader(Bundle bundle) {
        return getWiring(bundle).getClassLoader();
    }
}
