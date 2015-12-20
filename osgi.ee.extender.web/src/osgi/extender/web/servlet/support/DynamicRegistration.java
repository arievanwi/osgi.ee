/*
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
package osgi.extender.web.servlet.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.Registration;

/**
 * Registration of a specific object. Used as a base for servlet and filter registrations.
 *
 * @param <T> The type for which the registration takes place
 */
public class DynamicRegistration<T> implements Registration {
    private String registrationName;
    private Map<String, String> initParameters = new HashMap<>();
    private T object;
    private boolean async;

    @Override
    public String getClassName() {
        if (object == null) {
            return null;
        }
        return object.getClass().getName();
    }

    public T getObject() {
        return object;
    }

    public void setObject(T obj) {
        this.object = obj;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Map<String, String> getInitParameters() {
        return Collections.unmodifiableMap(initParameters);
    }

    public void setName(String n) {
        this.registrationName = n;
    }

    @Override
    public String getName() {
        return registrationName;
    }

    @Override
    public boolean setInitParameter(String parameter, String value) {
        if (parameter == null) {
            throw new IllegalStateException("parameter is null");
        }
        if (initParameters.containsKey(parameter)) {
            return false;
        }
        initParameters.put(parameter, value);
        return true;
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> map) {
        return map.entrySet().stream().
                filter((e) -> !setInitParameter(e.getKey(), e.getValue())).
                map((e) -> e.getKey()).
                collect(Collectors.toSet());
    }

    public void setAsyncSupported(boolean s) {
        this.async = s;
    }

    public boolean isAsyncSupported() {
        return this.async;
    }

    @Override
    public String toString() {
        return getName() + " - " + getObject();
    }
}