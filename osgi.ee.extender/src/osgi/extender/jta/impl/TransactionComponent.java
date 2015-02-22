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
package osgi.extender.jta.impl;

import java.util.Hashtable;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Component that starts the transaction manager if told so. It accepts a configuration to specify the rank
 * of the service. If this rank is lower than -1000, the service is not started.
 * 
 * @author Arie van Wijngaarden
 */
@Component(configurationPid = "osgi.extender.jta.tm", configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class TransactionComponent {
	private TransactionManagerImpl transactionManager;
	private ServiceRegistration<TransactionManager> registration;
	
	@Activate
	void activate(BundleContext context, Map<String, Object> properties) {
		int rank = 0;
		if (properties != null) {
			Object value = properties.get(Constants.SERVICE_RANKING);
			if (value != null) {
				rank = Integer.parseInt(value.toString());
			}
		}
		if (rank < -1000) return;
		transactionManager = new TransactionManagerImpl();
		Hashtable<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_RANKING, rank);
		registration = context.registerService(TransactionManager.class, transactionManager, dict);
	}
	
	@Deactivate
	void deactivate() {
		if (transactionManager != null) {
			try {
				registration.unregister();
			} catch (Exception exc) {}
			transactionManager.destroy();
		}
	}
}