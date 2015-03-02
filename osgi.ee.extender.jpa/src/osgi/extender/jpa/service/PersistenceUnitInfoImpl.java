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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Persistence unit information implementation.
 */
class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
    private PersistenceUnitDefinition definition;
    private Bundle unitBundle;
    private ClassLoader ppClassLoader;
    
    /**
     * Construct a persistence unit information object.
     * 
     * @param wrapped The bundle containing the definition of the persistence unit
     * @param ppLoader The class loader of the persistence provider bundle
     * @param definition The persistence unit definition
     */
    PersistenceUnitInfoImpl(Bundle wrapped, ClassLoader ppLoader, PersistenceUnitDefinition definition) {
        this.unitBundle = wrapped;
        this.definition = definition;
        this.ppClassLoader = ppLoader;
    }
    
    @Override
    public void addTransformer(ClassTransformer transformer) {
        System.out.println("WARNING: bytecode weaving using " + 
                transformer.getClass().getName() + " not performed for unit " + definition.name);
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return definition.excludeUnlisted;
    }

    @Override
    public ClassLoader getClassLoader() {
        // Get a class loader that is the combination of the 
        // persistence bundle class loader and the persistence provider class loader.
        return CompoundClassLoader.from(unitBundle.adapt(BundleWiring.class).getClassLoader(), ppClassLoader);
    }

    private List<URL> getUrlsFrom(Collection<String> in) {
        return in.stream().map((s) -> unitBundle.getEntry(s)).collect(Collectors.toList());
    }
    
    @Override
    public List<URL> getJarFileUrls() {
        return getUrlsFrom(definition.jarFiles);
    }

    @Override
    public DataSource getJtaDataSource() {
        return this.getFromDefinition(definition.jtaDs);
    }

    @Override
    public List<String> getManagedClassNames() {
        return definition.classes;
    }

    @Override
    public List<String> getMappingFileNames() {
        return definition.mappingFiles;
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        // I guess this one is only called when weaving is done.
        return getClassLoader();
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return getFromDefinition(definition.nonJtaDs);
    }

    @Override
    public String getPersistenceProviderClassName() {
        return (definition.provider.isEmpty()) ? null : definition.provider;
    }

    @Override
    public String getPersistenceUnitName() {
        return definition.name;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        // Used for class searching. Put it to the first entry in the classpath of the bundle
        // we are wrapping.
        String root = unitBundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
        if (root == null) {
            root = ".";
        }
        else {
            root = root.split(",")[0];
        }
        root.replace(".", "/");
        return unitBundle.getEntry(root);
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return definition.version;
    }

    @Override
    public Properties getProperties() {
        Properties props = new Properties();
        props.putAll(definition.properties);
        return props;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        if (definition.cachingType.isEmpty()) {
            return SharedCacheMode.UNSPECIFIED;
        }
        return SharedCacheMode.valueOf(definition.cachingType);
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        if (definition.transactionType.isEmpty()) {
            return PersistenceUnitTransactionType.JTA;
        }
        PersistenceUnitTransactionType type = 
                PersistenceUnitTransactionType.valueOf(definition.transactionType);
        return type;
    }

    @Override
    public ValidationMode getValidationMode() {
        if (definition.validationMode.isEmpty()) {
            return ValidationMode.AUTO;
        }
        return ValidationMode.valueOf(definition.validationMode);
    }

    /**
     * Get a datasource from a definition found in the unit definition.
     * 
     * @param def The definition found
     * @return A data source that matches the definition, or null if it
     * is not a data source definition recognised
     */
    private DataSource getFromDefinition(final String def) {
        final String SERVICEDEF = "osgi:service/";
        if (def == null || !def.startsWith(SERVICEDEF)) {
            return null; 
        }
        String remainder = def.substring(SERVICEDEF.length());
        int index = remainder.indexOf("/");
        String filter;
        if (index < 0) {
            filter = "(" + Constants.OBJECTCLASS + "=" + remainder + ")";
        }
        else {
            String className = remainder.substring(0, index);
            String subfilter = remainder.substring(index + 1);
            filter = "(&(" + Constants.OBJECTCLASS + "=" + className + ")" + subfilter + ")";
        }
        try {
            final ServiceTracker<DataSource, DataSource> tracker =
                    new ServiceTracker<>(unitBundle.getBundleContext(), FrameworkUtil.createFilter(filter), null);
            tracker.open();

            return (DataSource) Proxy.newProxyInstance(PersistenceUnitInfoImpl.this.getClassLoader(),
                    new Class<?>[] {DataSource.class},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            DataSource datasource = tracker.waitForService(1000L);
                            if (datasource == null) {
                                throw new RuntimeException("data source: " + def + " is not known as service");
                            }
                            return method.invoke(datasource, args);
                        }
                    });
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
    }
}
