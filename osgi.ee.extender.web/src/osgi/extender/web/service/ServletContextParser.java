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
package osgi.extender.web.service;

import java.net.URL;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.jcp.xmlns.xml.ns.javaee.FilterMappingType;
import org.jcp.xmlns.xml.ns.javaee.FilterType;
import org.jcp.xmlns.xml.ns.javaee.ParamValueType;
import org.jcp.xmlns.xml.ns.javaee.ServletMappingType;
import org.jcp.xmlns.xml.ns.javaee.ServletNameType;
import org.jcp.xmlns.xml.ns.javaee.ServletType;
import org.jcp.xmlns.xml.ns.javaee.UrlPatternType;
import org.jcp.xmlns.xml.ns.javaee.WebAppType;
import org.osgi.framework.Bundle;

import osgi.extender.web.WebContextDefinition;
import osgi.extender.web.servlet.OurServletContext;
import osgi.extender.web.servlet.support.FRegistration;
import osgi.extender.web.servlet.support.SRegistration;

/**
 * Parser for a web.xml like file somewhere in a bundle. Only the minimum number of elements are actually parsed, meaning that
 * descriptions, etc. are not handled at all since they are not required at all. The parser opens the URL and creates a new servlet
 * context from it containing the information.
 */
class ServletContextParser {
    static OurServletContext create(Bundle bundle, WebContextDefinition definition) throws Exception {
        OurServletContext context = new OurServletContext(bundle, definition.getContextPath(), definition.getResourceBase());
        if (definition.getDefinition() != null) {
            URL webxml = bundle.getEntry(definition.getDefinition());
            if (webxml == null) {
                throw new Exception("cannot find " + definition.getDefinition() + " for bundle: " + bundle);
            }
            parseFile(webxml, context);
        }
        return context;
    }

    /**
     * Perform some actions with a specific elements set from the webapp definition.
     *
     * @param elements The elements from the web-app definition
     * @param nameMatcher A matcher for the names
     * @param acceptor The consumer of the matching elements
     */
    private static void doWith(Collection<JAXBElement<?>> elements, Predicate<String> nameMatcher, Consumer<Object> acceptor) {
        elements.stream().filter((jbe) -> nameMatcher.test(jbe.getName().getLocalPart())).forEach((jbe) -> acceptor.accept(jbe.getValue()));
    }

    /**
     * Parse context parameters.
     *
     * @param elements The elements from the web definition
     * @param handler The servlet context that will in the end contain the information
     */
    private static void parseParameters(Collection<JAXBElement<?>> elements, OurServletContext handler) {
        doWith(elements, (name) -> "context-param".equals(name), (o) -> {
            ParamValueType parameter = (ParamValueType) o;
            handler.setInitParameter(parameter.getParamName().getValue(), parameter.getParamValue().getValue());
        });
    }

    /**
     * Parse the servlets from the context definition. This means: the definition of the servlet names and classes as well
     * as the mapping that is present for the servlets.
     *
     * @param elements The web app elements
     * @param handler The servlet context that accepts the information
     */
    private static void parseServlets(Collection<JAXBElement<?>> elements, OurServletContext handler) {
        doWith(elements, (n) -> "servlet".equals(n), (o) -> {
           ServletType type = (ServletType) o;
           SRegistration holder = handler.addServlet(type.getServletName().getValue(), type.getServletClass().getValue());
           type.getInitParam().forEach((parameter) ->
               holder.setInitParameter(parameter.getParamName().getValue(), parameter.getParamValue().getValue()));
        });
        doWith(elements, (n) -> "servlet-mapping".equals(n), (o) -> {
           ServletMappingType type = (ServletMappingType) o;
           String name = type.getServletName().getValue();
           ServletRegistration reg = handler.getServletRegistration(name);
           if (reg == null) {
               throw new RuntimeException("invalid servlet-mapping definition: servlet \"" + name + "s\" not found");
           }
           type.getUrlPattern().stream().map((ut) -> ut.getValue()).forEach((m) -> reg.addMapping(m));
        });
    }

    /**
     * Parse the filters from the context definition. This means: the filter names and classes as well as the mapping that is present
     * for the filters (to either servlets or url paths).
     *
     * @param elements The web app definition elements
     * @param handler The servlet context in which the definition is made
     */
    private static void parseFilters(Collection<JAXBElement<?>> elements, OurServletContext handler) {
        doWith(elements, (n) -> "filter".equals(n), (o) -> {
           FilterType filterType = (FilterType) o;
           FRegistration holder = handler.addFilter(filterType.getFilterName().getValue(), filterType.getFilterClass().getValue());
           filterType.getInitParam().forEach((parameter) ->
               holder.setInitParameter(parameter.getParamName().getValue(), parameter.getParamValue().getValue()));
           holder.setAsyncSupported(filterType.getAsyncSupported().isValue());
        });
        doWith(elements, (n) -> "filter-mapping".equals(n), (o) -> {
            FilterMappingType mapping = (FilterMappingType) o;
            String name = mapping.getFilterName().getValue();
            FilterRegistration reg = handler.getFilterRegistration(name);
            if (reg == null) {
                throw new RuntimeException("invalid filter-mapping definition: filter \"" + name + "\" not found");
            }
            List<String> urlMappings = mapping.getUrlPatternOrServletName().stream().
                filter((obj) -> UrlPatternType.class.isAssignableFrom(obj.getClass())).
                map((c) -> UrlPatternType.class.cast(o)).map((c) -> c.getValue()).collect(Collectors.toList());
            reg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, urlMappings.toArray(new String[urlMappings.size()]));
            List<String> servletMappings = mapping.getUrlPatternOrServletName().stream().
                    filter((obj) -> ServletNameType.class.isAssignableFrom(obj.getClass())).
                    map((c) -> ServletNameType.class.cast(o)).map((c) -> c.getValue()).collect(Collectors.toList());
            reg.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), true, servletMappings.toArray(new String[servletMappings.size()]));
        });
    }

    /**
     * Parse a web-app definition file.
     *
     * @param url The URL pointing to the web-app definition
     * @param handler The servlet context to fill
     * @throws Exception In case of errors
     */
    private static void parseFile(URL url, OurServletContext handler) throws Exception {
        JAXBContext context = JAXBContext.newInstance(WebAppType.class);
        Unmarshaller unm = context.createUnmarshaller();
        JAXBElement<?> element = (JAXBElement<?>) unm.unmarshal(url);
        WebAppType type = (WebAppType) element.getValue();
        Collection<JAXBElement<?>> elements = type.getModuleNameOrDescriptionAndDisplayName();
        parseParameters(elements, handler);
        parseServlets(elements, handler);
        parseFilters(elements, handler);
    }
}
