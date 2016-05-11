/*
 * Copyright 2016, Fujifilm Manufacturing Europe B.V.
 * Copyright 2016, aVineas IT Consulting
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * Persistence provider wrapper for hibernate. Actually just delegates to the
 * persistence provider from hibernate OSGi, but adds an additional property to
 * the service to make it OSGi enterprise specification compliant. Furthermore,
 * it tracks the bundles that are wrapped to allow the loading hook to add
 * dynamic imports for the packages required by hibernate for proxying.
 * 
 * @author Arie van Wijngaarden
 */
@Component(property = {
        EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER + "=" + PersistenceProviderImpl.PROVIDERID}, service = {
                PersistenceProvider.class, PersistenceProviderImpl.class})
public class PersistenceProviderImpl implements PersistenceProvider {
    static final String PROVIDERID = "org.hibernate.jpa.HibernatePersistenceProvider";
    private PersistenceProvider provider;
    private Set<Bundle> bundles = new HashSet<>();

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map props) {
        Bundle bundle = ((BundleReference) info.getClassLoader()).getBundle();
        synchronized (bundles) {
            bundles.add(bundle);
        }
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
    
    boolean hasBundle(Bundle bundle) {
        synchronized (bundles) {
            return bundles.contains(bundle);
        }
    }

    @Reference(target = "(javax.persistence.provider=" + PROVIDERID + ")")
    void setProvider(PersistenceProvider p) {
        this.provider = p;
    }
}