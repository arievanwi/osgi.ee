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

import org.eclipse.persistence.platform.server.ServerPlatformBase;
import org.eclipse.persistence.sessions.DatabaseSession;

/**
 * Our extension/implementation of a server platform for eclipselink.
 */
public class OurPlatForm extends ServerPlatformBase {

	public OurPlatForm(DatabaseSession newDatabaseSession) {
		super(newDatabaseSession);
		System.out.println("Platform created");
	}

	@Override
	public Class<?> getExternalTransactionControllerClass() {
		return TransactionController.class;
	}
}
