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
package osgi.jta.servlet.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.transaction.TransactionManager;
import javax.transaction.Status;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Transaction filter. Starts a transaction as part of a filter. Needs to be set as a filter
 * for a servlet to open and close transactions.
 */
public class TransactionFilter implements Filter {
	private ServiceTracker<TransactionManager, TransactionManager> tracker;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws ServletException {
		TransactionManager manager;
		try {
			manager = tracker.waitForService(2000L);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ServletException(ex);
		}
		try {
			manager.begin();
			chain.doFilter(request, response);
			if (manager.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
				manager.rollback();
			}
			else {
				manager.commit();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				manager.rollback();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void init(FilterConfig config) {
		BundleContext context = (BundleContext) config.getServletContext().getAttribute("osgi-bundlecontext");
		tracker = new ServiceTracker<>(context, TransactionManager.class, null);
		tracker.open();
	}
	
	@Override
	public void destroy() {
		tracker.close();
	}
}
