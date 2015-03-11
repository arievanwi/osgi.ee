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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Basic class for data source providing. Takes care of property handling, registration, etc. and
 * can be extended where needed.
 */
abstract class BasicDataSourceProvider<T extends BasicDataSource> {
    
    private Map<String, Registration<T>> registrations = new HashMap<>();
    
    /**
     * Perform a registration for a specific PID. The properties specified are converted to settings
     * on a basic datasource and that one is registered in the service registry for later handling.
     * Note that no checking is done on validation of properties, they are parsed as is.
     * 
     * @param pid The managed service pid
     * @param properties The properties of the pid
     */
    synchronized void registration(String pid, Dictionary<String, ?> properties) {
        deleted(pid);
        Hashtable<String, Object> dict = new Hashtable<>();
        Enumeration<String> enumeration = properties.keys();
        T source = getDataSource();
        source.setMaxWait(10000L);
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            Object value = properties.get(key);
            // Check the values.
            if (key.equals("jdbc.driver")) {
                source.setDriverClassName(value.toString());
            }
            else if (key.equals("jdbc.user")) {
                source.setUsername(value.toString());
            }
            else if (key.equals("jdbc.password")) {
                source.setPassword(value.toString());
            }
            else if (key.equals("jdbc.url")) {
                source.setUrl(value.toString());
            }
            else if (key.equals("validation.query")) {
                source.setValidationQuery(value.toString());
            }
            else if (key.equals("validation.timeout")) {
                source.setValidationQueryTimeout(Integer.parseInt(value.toString()));
            }
            else if (key.equals("pool.idle.min")) {
                source.setMinIdle(Integer.parseInt(value.toString()));
            }
            else if (key.equals("pool.idle.max")) {
                source.setMaxIdle(Integer.parseInt(value.toString()));
            }
            else if (key.equals("pool.wait")) {
                source.setMaxWait(Long.parseLong(value.toString()));
            }
            else if (key.equals("pool.active.max")) {
                source.setMaxActive(Integer.parseInt(value.toString()));
            }
            else {
                dict.put(key, value);
            }
        }
        // Got it. Create the registration for it.
        ServiceRegistration<DataSource> sr = FrameworkUtil.getBundle(getClass()).
                getBundleContext().registerService(DataSource.class, source, dict);
        registrations.put(pid, new Registration<>(source, sr));
    }
    
    /**
     * Actually meant for the subclasses to automatically handle the managed service factory methods.
     * 
     * @param pid The managed services pid
     * @param properties The properties
     */
    public void updated(String pid, Dictionary<String, ?> properties) {
        registration(pid, properties);
    }

    /**
     * Actually meant for the subclasses to automatically handle the managed service factory methods.
     * 
     * @param pid The managed service pid
     */
    public void deleted(String pid) {
        Registration<T> reg = registrations.get(pid);
        if (reg != null)
            destroy(reg);
    }

    @Deactivate
    void deactivate() {
        registrations.values().stream().forEach((r) -> destroy(r));
    }
    
    /**
     * Destroy a registration. Normally only done when this bundle is stopped. However,
     * it is possible to update the properties which will cause an immediate effect on the
     * datasource. However, whether this works correctly is questionable at best.
     * 
     * @param reg The registration
     */
    private static <T extends BasicDataSource> void destroy(Registration<T> reg) {
        reg.registration.unregister();
        try {
            reg.dataSource.close();
        } catch (Exception exc) {}
    }
    
    abstract T getDataSource();
    
    static class Registration<T extends BasicDataSource> {
        final ServiceRegistration<DataSource> registration;
        final T dataSource;
        Registration(T ds, ServiceRegistration<DataSource> sr) {
            this.registration = sr;
            this.dataSource = ds;
        }
    }
}
