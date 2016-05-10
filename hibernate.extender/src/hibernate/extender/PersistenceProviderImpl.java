/*
 * Copyright 2016, Fujifilm Manufacturing Europe B.V.
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
package hibernate.extender;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

@Component(property = {EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER + "=" + PersistenceProviderImpl.PROVIDERID})
public class PersistenceProviderImpl implements PersistenceProvider {
    static final String PROVIDERID = "org.hibernate.jpa.HibernatePersistenceProvider";
    private PersistenceProvider provider;

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map props) {
        return provider.createContainerEntityManagerFactory(info, props);
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(String info, Map props) {
        return provider.createEntityManagerFactory(info, props);
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map props) {
        provider.generateSchema(info, props);
    }

    @Override
    public boolean generateSchema(String unit, Map props) {
        return provider.generateSchema(unit, props);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return provider.getProviderUtil();
    }
    
    @Reference(target = "(javax.persistence.provider=" + PROVIDERID + ")")
    void setProvider(PersistenceProvider p) {
        this.provider = p;
    }
}