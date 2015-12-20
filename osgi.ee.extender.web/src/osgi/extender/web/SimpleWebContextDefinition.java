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
package osgi.extender.web;

/**
 * Simple implementation of a web context definition.
 */
public class SimpleWebContextDefinition implements WebContextDefinition {
    private String contextPath;
    private String definition;
    private String resourceBase;

    public SimpleWebContextDefinition(String p, String d, String r) {
        contextPath = p;
        definition = d;
        resourceBase = r;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getDefinition() {
        return definition;
    }

    @Override
    public String getResourceBase() {
        return resourceBase;
    }
}
