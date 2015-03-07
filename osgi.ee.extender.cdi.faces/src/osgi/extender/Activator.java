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
package osgi.extender;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.BundleTracker;

import osgi.extender.resource.impl.ResourceHandlingBundleListener;

/**
 * Bundle activator. Takes care of resource handling.
 * 
 * @author Arie van Wijngaarden
 */
public class Activator implements BundleActivator {
    private BundleTracker<Object> tracker;
    
    @Override
    public void start(BundleContext context) {
        tracker = new BundleTracker<>(context, Bundle.ACTIVE, new ResourceHandlingBundleListener());
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) {
        tracker.close();
    }
}
