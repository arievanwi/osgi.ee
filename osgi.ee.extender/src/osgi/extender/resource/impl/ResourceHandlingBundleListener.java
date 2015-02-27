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
package osgi.extender.resource.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import osgi.extender.resource.BundleResourceProvider;

/**
 * Bundle listener that checks bundles and exports resources on behalf of them if the
 * bundle indicates this. A bundle can register its exporting requirement by specifying
 * a "Bundle-Resources" header that indicates which parts to export.
 */
public class ResourceHandlingBundleListener implements BundleTrackerCustomizer<Object> {
    private static final String HEADER = "Bundle-Resources";
    
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        // See if the bundle has a Bundle-Resources indication.
        String value = bundle.getHeaders().get(HEADER);
        if (value == null) return null;
        // Process the header. It has a format like:
        // resources, lib=resources/lib, etc. Basically the mapping to be used.
        String[] firstRun = value.split(";");
        Hashtable<String, Object> dict = new Hashtable<>();
        if (firstRun.length > 1) {
            value = firstRun[0];
            // Process the service registration properties.
            Arrays.asList(firstRun[1].split(",")).stream().
                map((s) -> s.split("=")).
                forEach((sa) -> {
                    if (sa.length == 2) {
                        dict.put(sa[0].trim(), sa[1].trim());
                    }
                });
        }
        String[] identifiers = value.split("[,]");
        final Map<String, String> mapping = new HashMap<String, String>();
        mapping.put(BundleResourceProvider.DEFAULT, "/");
        Arrays.asList(identifiers).stream().
            map((s) -> { // Convert to key/value pairs in an array of 2 strings
                String[] values = s.trim().split("=");
                if (values.length == 1) {
                    values = new String[]{BundleResourceProvider.DEFAULT, values[0]};
                }
                else {
                    values[0] = values[0].trim();
                    values[1] = values[1].trim();
                }
                return values;
            }).
            forEach((a) -> {  // Make sure that the path starts with a slash and ends with one.
                String v = a[1];
                if (!v.startsWith("/")) {
                    v = "/" + v;
                }
                if (!v.endsWith("/")) {
                    v = v + "/";
                }
                mapping.put(a[0], v);
            });
        OurBundleResourceProvider provider = new OurBundleResourceProvider(bundle, mapping);
        BundleContext context = bundle.getBundleContext();
        dict.put(Constants.BUNDLE_SYMBOLICNAME, context.getBundle().getSymbolicName());
        String cat = context.getBundle().getHeaders().get(Constants.BUNDLE_CATEGORY);
        if (cat != null)
            dict.put(Constants.BUNDLE_CATEGORY, cat);
        ServiceRegistration<BundleResourceProvider> sr = context.registerService(BundleResourceProvider.class, provider, dict);
        return sr;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object sr) {
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object sr) {
        if (sr != null) {
            try {
                ServiceRegistration<?> reg = (ServiceRegistration<?>) sr;
                reg.unregister();
            } catch (Exception exc) {}
        }
    }
}