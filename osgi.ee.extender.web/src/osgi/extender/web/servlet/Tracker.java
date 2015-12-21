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
package osgi.extender.web.servlet;

import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tracker customizer for filters and servlets: picks up dynamically registered filters and adds them to the
 * context.
 */
class Tracker<T> implements ServiceTrackerCustomizer<T, String> {
    private BundleContext bundleContext;
    private Function<T, String> adder;
    private Consumer<String> remover;

    Tracker(BundleContext context, Function<T, String> adder, Consumer<String> remover) {
        this.bundleContext = context;
        this.adder = adder;
        this.remover = remover;
    }

    @Override
    public String addingService(ServiceReference<T> ref) {
        T obj = bundleContext.getService(ref);
        if (obj == null) {
            return null;
        }
        String name = adder.apply(obj);
        if (name == null) {
            bundleContext.ungetService(ref);
        }
        return name;
    }

    @Override
    public void modifiedService(ServiceReference<T> ref, String name) {
    }

    @Override
    public void removedService(ServiceReference<T> ref, String name) {
        bundleContext.ungetService(ref);
        remover.accept(name);
    }
}
