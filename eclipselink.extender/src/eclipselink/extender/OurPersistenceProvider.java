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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.eclipse.persistence.config.PersistenceUnitProperties;

/**
 * Extender class for eclipse link persistence provider.
 */
public class OurPersistenceProvider extends org.eclipse.persistence.jpa.PersistenceProvider {
    
    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(
            PersistenceUnitInfo info, @SuppressWarnings("rawtypes") Map properties) {
        Map<String, Object> props = new HashMap<>();
        if (properties != null) {
            props.putAll(props);
        }
        // Check if we are using JTA. If so, overwrite the platform.
        if (PersistenceUnitTransactionType.JTA.equals(info.getTransactionType())) {
            props.put(PersistenceUnitProperties.TARGET_SERVER, OurPlatForm.class.getName());
            if (!SharedCacheMode.NONE.equals(info.getSharedCacheMode())) {
                System.out.println("WARNING: Second level caching in JTA mode doesn't always work correctly");
            }
        }
        return super.createContainerEntityManagerFactory(info, props);
    }    
}
