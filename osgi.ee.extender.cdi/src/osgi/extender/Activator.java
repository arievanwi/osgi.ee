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

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import osgi.extender.cdi.weld.CdiBundleChangeListener;
import osgi.extender.resource.impl.ResourceHandlingBundleListener;

/**
 * Bundle activator. Only takes care of the bundle tracking stuff. All other functionality
 * is handled via SCR components. As such this bundle requires a SCR.
 *
 * @author Arie van Wijngaarden
 */
public class Activator implements BundleActivator {
    private Stream<BundleTracker<Object>> trackers;

    @Override
    public void start(BundleContext context) {
        Collection<BundleTrackerCustomizer<Object>> handlers =
                Arrays.asList(new CdiBundleChangeListener(context.getBundle()),
                        new ResourceHandlingBundleListener());
        trackers = handlers.stream().map((c) ->
        new BundleTracker<>(context, Bundle.ACTIVE, c));
        new Thread(() -> trackers.forEach((t) -> t.open())).start();
    }

    @Override
    public void stop(BundleContext context) {
        trackers.forEach((t) -> {
            try {
                t.close();
            } catch (Exception e) {}
        });
    }
}