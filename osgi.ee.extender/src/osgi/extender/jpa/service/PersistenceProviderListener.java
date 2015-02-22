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
package osgi.extender.jpa.service;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.spi.PersistenceProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Listener for persistence providers. Tracks the persistence providers in the container and reports
 * the changes to a listener. It provides an interface for retrieving a provider by name.
 */
public class PersistenceProviderListener implements 
		ServiceTrackerCustomizer<PersistenceProvider, PersistenceProvider>, PPProvider {
	private Map<String, PersistenceProvider> tracked = new HashMap<>();
	private BundleContext bundleContext;
	private PPListener listener;
	
	public PersistenceProviderListener(BundleContext context, PPListener listener) {
		this.bundleContext = context;
		this.listener = listener;
	}

	private static String getName(ServiceReference<?> ref) {
		String providerName = (String) ref.getProperty(EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER);
		return providerName;
	}
	
	@Override
	public PersistenceProvider addingService(ServiceReference<PersistenceProvider> ref) {
		String providerName = getName(ref);
		if (providerName == null) return null;
		PersistenceProvider provider = bundleContext.getService(ref);
		if (provider != null) {
			synchronized (tracked) {
				tracked.put(providerName, provider);
			}
			listener.added(providerName, provider);
		}
		return provider;
	}

	@Override
	public void modifiedService(ServiceReference<PersistenceProvider> ref,
			PersistenceProvider provider) {
	}
	
	@Override
	public void removedService(ServiceReference<PersistenceProvider> ref, PersistenceProvider provider) {
		String name = getName(ref);
		PersistenceProvider prov;
		synchronized (tracked) {
			prov = tracked.remove(name);
		}
		if (prov != null) {
			bundleContext.ungetService(ref);
			listener.removed(name);
		}
	}

	@Override
	public Map.Entry<String, PersistenceProvider> get(String name) {
		PersistenceProvider provider = tracked.get(name);
		Map.Entry<String, PersistenceProvider> entry = null;
		if (provider != null) {
			entry = new AbstractMap.SimpleEntry<String, PersistenceProvider>(name, provider);
		}
		else if (name == null || name.trim().length() == 0 && tracked.size() > 0) {
			entry = tracked.entrySet().iterator().next();
		}
		return entry;
	}
}