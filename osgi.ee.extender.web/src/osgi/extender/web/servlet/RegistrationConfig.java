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
package osgi.extender.web.servlet;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import osgi.extender.web.servlet.support.DynamicRegistration;

/**
 * Abstraction of a configuration object that receives a registration object as input.
 */
public class RegistrationConfig implements FilterConfig, ServletConfig {
    private DynamicRegistration<?> registration;
    private ServletContext context;

    RegistrationConfig(DynamicRegistration<?> reg, ServletContext c) {
        registration = reg;
        context = c;
    }

    @Override
    public String getInitParameter(String name) {
        return registration.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(registration.getInitParameters().keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }

    @Override
    public String getServletName() {
        return registration.getName();
    }

    @Override
    public String getFilterName() {
        return registration.getName();
    }
}
