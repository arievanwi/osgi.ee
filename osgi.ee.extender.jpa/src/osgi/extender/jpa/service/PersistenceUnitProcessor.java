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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.osgi.framework.Bundle;

/**
 * Persistence unit processor. Processes one persistence unit URL to a real
 * entity manager factory.
 */
class PersistenceUnitProcessor {
    static List<PersistenceUnitDefinition> getDefinitions(Bundle wrapping, String name) {
        URL url = wrapping.getEntry(name);
        List<PersistenceUnitDefinition> out = new ArrayList<>();
        if (url == null) return out;
        try (InputStream stream = url.openStream()) {
            return PersistenceUnitDefinition.fromFile(stream);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public static PersistenceUnitInfo getPersistenceUnitInfo(Bundle bundle,
            PersistenceUnitDefinition definition,
            PersistenceProvider provider) {
        return new PersistenceUnitInfoImpl(bundle, provider.getClass().getClassLoader(), definition);
    }
    
    public static EntityManagerFactory createFactory(PersistenceProvider provider, PersistenceUnitInfo info) {
        Map<String, String> properties = new HashMap<>();
        return provider.createContainerEntityManagerFactory(info, properties);
    }
}
