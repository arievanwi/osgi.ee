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
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import osgi.extender.web.WebContextDefinition;

/**
 * Tracker customizer for filters and servlets: picks up dynamically registered filters and adds them to the
 * context.
 */
class Tracker<T, R> implements ServiceTrackerCustomizer<T, R> {
    private BundleContext bundleContext;
    private Function<T, R> adder;
    private Consumer<R> remover;
    private String context;

    Tracker(BundleContext bc, String context, Function<T, R> adder, Consumer<R> remover) {
        this.bundleContext = bc;
        this.adder = adder;
        this.remover = remover;
        this.context = context;
    }

    @Override
    public R addingService(ServiceReference<T> ref) {
        Object contextValue = ref.getProperty(WebContextDefinition.WEBCONTEXTPATH);
        if (contextValue == null) {
            return null;
        }
        try {
            if (!Pattern.matches(contextValue.toString(), context)) {
                return null;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
        T obj = bundleContext.getService(ref);
        if (obj == null) {
            return null;
        }
        R name = adder.apply(obj);
        if (name == null) {
            bundleContext.ungetService(ref);
        }
        return name;
    }

    @Override
    public void modifiedService(ServiceReference<T> ref, R name) {
        // We don't handle property changes.
    }

    @Override
    public void removedService(ServiceReference<T> ref, R name) {
        bundleContext.ungetService(ref);
        remover.accept(name);
    }
}
