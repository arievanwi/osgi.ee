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
package eclipselink.extender;

import javax.transaction.TransactionManager;

import org.eclipse.persistence.transaction.JTATransactionController;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Transaction controller we are using for setting the transaction manager. The transaction
 * managers are tracked using a service tracker and set to the internally managed
 * transaction manager when one appears or disappear.
 * 
 * @author Arie van Wijngaarden
 */
public class TransactionController extends JTATransactionController {
	private static ServiceTracker<TransactionManager, TransactionManager> tracker;
	
	static void initialize(BundleContext context) {
		tracker = new ServiceTracker<>(context, TransactionManager.class, null); 
		tracker.open();
	}

	@Override
	protected TransactionManager acquireTransactionManager() throws Exception {
		TransactionManager manager = tracker.waitForService(2000L);
		System.out.println("Transaction manager: " + manager);
		return manager;
	}
	
	static void destroy() {
		tracker.close();
	}
}
