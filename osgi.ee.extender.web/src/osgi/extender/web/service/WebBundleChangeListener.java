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
package osgi.extender.web.service;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import osgi.extender.web.SimpleWebContextDefinition;
import osgi.extender.web.WebContextDefinition;

/**
 * Bundle change listener for web bundles: checks whether a bundle contains a web directory and
 * if so, extends the bundle for web handling.
 */
public class WebBundleChangeListener implements BundleTrackerCustomizer<ServiceRegistration<?>> {
    private static final String WEBXML = "/WEB-INF/web.xml";

    @Override
    public ServiceRegistration<?> addingBundle(Bundle bundle, BundleEvent event) {
        // Does the bundle contain a header according to the specification?
        String contextPath = bundle.getHeaders().get("Web-ContextPath");
        if (contextPath == null) {
            return null;
        }
        // Load the web.xml. In practice this is needed, although the specification allows running without.
        URL webxml = bundle.getEntry(WEBXML);
        SimpleWebContextDefinition def = new SimpleWebContextDefinition(contextPath, webxml == null ? null : WEBXML, "/");
        // Register the service to have our listener pick it up.
        ServiceRegistration<?> reg = bundle.getBundleContext().registerService(WebContextDefinition.class, def, null);
        return reg;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<?> object) {
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<?> object) {
        try {
            object.unregister();
        } catch (Exception exc) {}
    }
}
