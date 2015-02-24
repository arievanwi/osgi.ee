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
package datasource;

import javax.transaction.TransactionManager;

import org.apache.commons.dbcp.managed.BasicManagedDataSource;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provider of transactional data sources. Is a standard extension to the normal data source, however it 
 * requires a transaction manager to be present to handle the standard pooling.
 */
@Component(immediate = true, property = "service.pid=XAdatasource")
public class TransactionalDataSourceProvider extends BasicDataSourceProvider<BasicManagedDataSource> implements ManagedServiceFactory {
	private TransactionManager transactionManager;
	
	@Override
	public String getName() {
		return "XAdatasource";
	}

	@Reference
	void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	@Override
	BasicManagedDataSource getDataSource() {
		BasicManagedDataSource ds = new BasicManagedDataSource();
		ds.setTransactionManager(transactionManager);
		return ds;
	}
}