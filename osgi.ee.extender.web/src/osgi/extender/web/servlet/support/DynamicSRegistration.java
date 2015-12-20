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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;

/**
 * Dynamic servlet registration implementation. Extends a normal servlet registration with
 * some additional functionality that has no use in this functionality.
 */
public class DynamicSRegistration extends SRegistration implements ServletRegistration.Dynamic {

    @Override
    public void setLoadOnStartup(int los) {
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement elem) {
    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement elem) {
        return new HashSet<>();
    }
}
