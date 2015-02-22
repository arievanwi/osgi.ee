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

import javax.persistence.spi.PersistenceProvider;

/**
 * Interface, internally used to handle the interaction between the bundle tracking part and the persistence provider
 * part. This is the signaling of changes to the persistence providers available in the system.
 */
public interface PPListener {
	/**
	 * Signal that a persistence provider became available.
	 * 
	 * @param providerName The name of the provider
	 * @param provider The provider itself
	 */
	public void added(String providerName, PersistenceProvider provider);
	/**
	 * Signal that a persistence provider went out of scope.
	 * 
	 * @param providerName The name of the provider
	 */
	public void removed(String providerName);
}
