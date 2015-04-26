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

import java.util.Dictionary;

import javax.transaction.TransactionManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Component that starts the transaction manager, which is does by default unless a system property 
 * is set to disable it. The timeout can be configured via the configuration manager, but has a default
 * value in absence of a configuration manager.
 * 
 * @author Arie van Wijngaarden
 */
@Component(property = Constants.SERVICE_PID + "=" + TransactionComponent.PID, immediate = true)
public class TransactionComponent implements ManagedService {
    static final String PID = "osgi.extender.jta.tm";
    private TransactionManagerImpl transactionManager;
    private ServiceRegistration<TransactionManager> registration;
    
    @Activate
    synchronized void activate(BundleContext context) {
        String activate = System.getProperty(PID);
        if (activate == null || Boolean.parseBoolean(activate)) {
            int time = 30;
            transactionManager = new TransactionManagerImpl(time);
            registration = context.registerService(TransactionManager.class, transactionManager, null);
        }
    }
    
    @Deactivate
    synchronized void deactivate() {
        if (transactionManager != null) {
            try {
                registration.unregister();
            } catch (Exception exc) {}
            transactionManager.destroy();
        }
    }

    @Override
    public synchronized void updated(Dictionary<String, ?> dict) {
        if (dict == null || transactionManager == null) return;
        Object timer = dict.get(PID);
        if (timer != null) {
            int timeout = Integer.parseInt(timer.toString());
            try {
                transactionManager.setTransactionTimeout(timeout);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }
}