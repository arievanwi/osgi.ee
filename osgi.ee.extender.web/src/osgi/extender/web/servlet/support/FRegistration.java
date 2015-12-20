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
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;

/**
 * Filter registration implementation.
 */
public class FRegistration extends DynamicRegistration<Filter> implements FilterRegistration {
    private List<StringDispatcherPair> servletMapping = new ArrayList<>();
    private List<StringDispatcherPair> urlMapping = new ArrayList<>();

    private static void addTo(List<StringDispatcherPair> list, EnumSet<DispatcherType> types, boolean after, String... strings) {
        List<StringDispatcherPair> toAdd = Arrays.asList(strings).stream().
                map((s) -> new StringDispatcherPair(s, types)).
                collect(Collectors.toList());
        int index = after ? list.size() : 0;
        list.addAll(index, toAdd);
    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> types, boolean after, String... names) {
        addTo(servletMapping, types, after, names);
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> types, boolean after, String... patt) {
        addTo(urlMapping, types, after, patt);
    }

    @Override
    public Collection<String> getServletNameMappings() {
        return servletMapping.stream().map((p) -> p.string).collect(Collectors.toList());
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return urlMapping.stream().map((p) -> p.string).collect(Collectors.toList());
    }

    static class StringDispatcherPair {
        final String string;
        final EnumSet<DispatcherType> types;

        StringDispatcherPair(String n, EnumSet<DispatcherType> t) {
            string = n;
            types = t;
        }
    }
}
