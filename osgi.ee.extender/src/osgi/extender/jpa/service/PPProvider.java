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

import java.util.Map;

import javax.persistence.spi.PersistenceProvider;

/**
 * Provider of a persistence provider (clear?). Part that allows to retrieve an available persistence provider
 * by its name.
 */
public interface PPProvider {
    /**
     * Get a persistence provider by its name.
     * 
     * @param name The name of the persistence provider. May be a null or empty string
     * @return The persistence provider that is the best match given the requirements as name, value pair
     */
    public Map.Entry<String, PersistenceProvider> get(String name);
}
