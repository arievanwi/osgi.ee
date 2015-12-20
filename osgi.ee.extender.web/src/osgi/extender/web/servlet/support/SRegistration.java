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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;

/**
 * Servlet registration.
 */
public class SRegistration extends DynamicRegistration<Servlet> implements ServletRegistration {
    private String runAs;
    private List<String> mapping = new ArrayList<>();

    private boolean addMapping(String m) {
        if (mapping.contains(m)) {
            return false;
        }
        mapping.add(m);
        return true;
    }

    @Override
    public Set<String> addMapping(String... maps) {
        return Arrays.asList(maps).stream().filter((s) -> !addMapping(s)).collect(Collectors.toSet());
    }

    @Override
    public Collection<String> getMappings() {
        return Collections.unmodifiableCollection(mapping);
    }

    @Override
    public String getRunAsRole() {
        return runAs;
    }

    public void setRunAsRole(String s) {
        runAs = s;
    }
}
