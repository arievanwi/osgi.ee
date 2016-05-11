/*
 * Copyright 2016, aVineas IT Consulting
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
package hibernate.extender;

import java.util.Arrays;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Framework weaving hook to add imports to the proxy packages for the classes
 * loaded from the bundles that are extended via the hibernate persistence
 * provider.
 */
class LoadingHook implements WeavingHook {
    private static final List<String> imports = Arrays.asList("org.hibernate.proxy", "javassist.util.proxy");
    private ServiceTracker<PersistenceProviderImpl, PersistenceProviderImpl> tracker;

    LoadingHook(BundleContext context) {
        tracker = new ServiceTracker<>(context, PersistenceProviderImpl.class, null);
        tracker.open();
    }

    private boolean hasBundle(Bundle bundle) {
        PersistenceProviderImpl pp = tracker.getService();
        if (pp == null)
            return false;
        return pp.hasBundle(bundle);
    }

    @Override
    public void weave(WovenClass clz) {
        if (!hasBundle(clz.getBundleWiring().getBundle()))
            return;
        if (!clz.getDynamicImports().containsAll(imports)) {
            clz.getDynamicImports().addAll(imports);
        }
    }
}