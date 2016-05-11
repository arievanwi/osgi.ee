/*
 * Copyright 2016, Fujifilm Manufacturing Europe B.V.
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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.weaving.WeavingHook;

public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) {
        LoadingHook hook = new LoadingHook(context);
        context.registerService(WeavingHook.class, hook, null);
    }

    @Override
    public void stop(BundleContext context) {
    }
}
