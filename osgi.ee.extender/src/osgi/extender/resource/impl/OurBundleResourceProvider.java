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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.Bundle;

import osgi.extender.resource.BundleResource;
import osgi.extender.resource.BundleResourceProvider;

/**
 * Implementation of a resource provider for bundles. Just keeps a mapping of
 * logical directories to prefixes in the bundle.
 */
class OurBundleResourceProvider implements BundleResourceProvider {
    private Bundle bundle;
    private Map<String, String> mapping;
    
    public OurBundleResourceProvider(Bundle context, Map<String, String> mapping) {
        this.bundle = context;
        this.mapping = mapping;
    }

    private String prefix(String dir) {
        String prefix = mapping.get(dir);
        if (prefix == null) return null;
        return prefix;
    }
    
    @Override
    public Collection<BundleResource> getResources(String dir) {
        ArrayList<BundleResource> resources = new ArrayList<BundleResource>();
        String prefix = prefix(dir);
        if (prefix == null) return null;
        // Actually a directory lookup.
        Enumeration<String> entries = bundle.getEntryPaths(prefix);
        if (entries != null) {
            while (entries.hasMoreElements()) {
                String next = entries.nextElement();
                resources.add(new OurBundleResource(bundle, next));
            }
        }
        return resources;
    }

    @Override
    public BundleResource getResource(String directory, String p) {
        String prefix = prefix(directory);
        if (prefix == null) return null;
        // Check the path.
        String path = p;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        // Check if it is a directory entry.
        String fullPath = prefix + path;
        if (bundle.getEntryPaths(fullPath) != null) return null;
        // Return the resource, if it exists.
        OurBundleResource source = new OurBundleResource(bundle, fullPath);
        if (source.getURL() == null) return null;
        return source;
    }
    
}
