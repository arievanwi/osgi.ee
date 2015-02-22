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
package osgi.extender.cdi.extension;

import org.jboss.weld.bootstrap.spi.Metadata;

/**
 * Meta data for our extension. IMHO a really unnecessary overhead.
 */
public class OurMetaData<T> implements Metadata<T> {
    private String location;
    private T value;
    
    public OurMetaData(String location, T value) {
        this.location = location;
        this.value = value;
    }
    
    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public T getValue() {
        return value;
    }
}
