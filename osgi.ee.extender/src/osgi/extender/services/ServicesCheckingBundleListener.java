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
package osgi.extender.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Listener for bundle changes that results in a list of services from the bundle, if any.
 */
public class ServicesCheckingBundleListener implements BundleTrackerCustomizer<Object> {

    private static ServiceRegistration<?> create(String interfaceName, String line, Bundle bundle) {
        ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
        String[] words = line.split("\\s+");
        Hashtable<String, Object> properties = new Hashtable<>();
        String className = words[0];
        for (int cnt = 1; cnt < words.length; cnt++) {
            String[] p = words[cnt].split("\\s*=\\s*");
            if (p.length == 2) {
                properties.put(p[0], p[1]);
            }
        }
        try {
            Object obj = loader.loadClass(className).newInstance(); 
            ServiceRegistration<?> sr = bundle.getBundleContext().registerService(interfaceName, obj, properties);
            return sr;
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
    }
    
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        // Check the bundle for services.
        final String SERVICESPATH = "/OSGI-INF/services";
        Enumeration<URL> en = bundle.findEntries(SERVICESPATH, "*", false);
        if (en == null) return null;
        List<ServiceRegistration<?>> references = new ArrayList<>();
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            String interfaceName = url.toString().substring(url.toString().lastIndexOf("/") + 1);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                reader.lines().
                    map((line) -> create(interfaceName, line, bundle)).
                    filter((sr) -> sr != null).
                    forEach((sr) -> references.add(sr));
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        return references;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        @SuppressWarnings("unchecked")
        List<ServiceRegistration<?>> regs = (List<ServiceRegistration<?>>) object;
        if (regs != null) {
            regs.stream().forEach((r) -> {
                try {
                    r.unregister();
                } catch (Exception exc) {}
            });
        }
    }
}
