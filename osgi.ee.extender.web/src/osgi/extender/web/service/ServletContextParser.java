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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jcp.xmlns.xml.ns.javaee.ErrorPageType;
import org.jcp.xmlns.xml.ns.javaee.FilterMappingType;
import org.jcp.xmlns.xml.ns.javaee.FilterType;
import org.jcp.xmlns.xml.ns.javaee.ListenerType;
import org.jcp.xmlns.xml.ns.javaee.ParamValueType;
import org.jcp.xmlns.xml.ns.javaee.ServletMappingType;
import org.jcp.xmlns.xml.ns.javaee.ServletNameType;
import org.jcp.xmlns.xml.ns.javaee.ServletType;
import org.jcp.xmlns.xml.ns.javaee.SessionConfigType;
import org.jcp.xmlns.xml.ns.javaee.UrlPatternType;
import org.jcp.xmlns.xml.ns.javaee.WebAppType;
import org.jcp.xmlns.xml.ns.javaee.WelcomeFileListType;
import org.osgi.framework.Bundle;

import osgi.extender.helpers.DelegatingClassLoader;
import osgi.extender.web.WebContextDefinition;
import osgi.extender.web.servlet.DispatchingServlet;
import osgi.extender.web.servlet.OurServletContext;
import osgi.extender.web.servlet.support.FRegistration;
import osgi.extender.web.servlet.support.SRegistration;

import com.sun.java.xml.ns.javaee.ObjectFactory;
import com.sun.java.xml.ns.javaee.TldTaglibType;

/**
 * Parser for a web.xml like file somewhere in a bundle. Only the minimum number of elements are actually parsed, meaning that
 * descriptions, etc. are not handled at all since they are not required at all. The parser opens the URL and creates a new servlet
 * context from it containing the information.
 */
class ServletContextParser {
    static DispatchingServlet create(Bundle bundle, WebContextDefinition definition) throws Exception {
        OurServletContext context = new OurServletContext(bundle, definition.getContextPath(),
                definition.getResourceBase());
        Map<Integer, String> httpErrorPages = new HashMap<>();
        Map<Class<?>, String> exceptionPages = new LinkedHashMap<>();
        List<String> welcomePages = new ArrayList<>();
        if (definition.getDefinition() != null) {
            URL webxml = bundle.getEntry(definition.getDefinition());
            if (webxml == null) {
                throw new Exception("cannot find " + definition.getDefinition() + " for bundle: " + bundle);
            }
            parseFile(webxml, context, welcomePages, httpErrorPages, exceptionPages);
        }
        loadTLDListeners(bundle).forEach((l) -> context.addListener(l));
        return new DispatchingServlet(context, welcomePages, httpErrorPages, exceptionPages);
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
                map((c) -> UrlPatternType.class.cast(c)).map((c) -> c.getValue()).collect(Collectors.toList());
            reg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, urlMappings.toArray(new String[urlMappings.size()]));
            List<String> servletMappings = mapping.getUrlPatternOrServletName().stream().
                    filter((obj) -> ServletNameType.class.isAssignableFrom(obj.getClass())).
                    map((c) -> ServletNameType.class.cast(c)).map((c) -> c.getValue()).collect(Collectors.toList());
            reg.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), true, servletMappings.toArray(new String[servletMappings.size()]));
        });
    }

    /**
     * Parse the listener classes from the servlet context.
     *
     * @param elements The web-app elements
     * @param handler The context that receives the listeners
     */
    private static void parseListeners(Collection<JAXBElement<?>> elements, OurServletContext handler) {
        doWith(elements, (n) -> "listener".equals(n), (o) -> {
            ListenerType listener = (ListenerType) o;
            String clz = listener.getListenerClass().getValue();
            handler.addListener(clz);
        });
    }

    private static String parseLocation(String location) {
        return location.startsWith("/") ? location : "/" + location;
    }

    /**
     * Parse the welcome file definitions and return them.
     *
     * @param elements The web app elements
     * @param welcomeFiles The welcome files, filled
     */
    private static void parseWelcomeFiles(Collection<JAXBElement<?>> elements,
            final Collection<String> welcomeFiles) {
        doWith(elements, (n) -> "welcome-file-list".equals(n), (o) -> {
            WelcomeFileListType welcomes = (WelcomeFileListType) o;
            welcomes.getWelcomeFile().forEach((w) -> welcomeFiles.add(parseLocation(w)));
        });
    }

    /**
     * Parse the error pages for exceptions and HTTP errors.
     *
     * @param elements The elements to parse
     * @param loader The class loader for class loading
     * @param errorPages Error pages map, filled
     * @param exceptionPages Exception pages map, filled
     */
    private static void parseErrorPages(Collection<JAXBElement<?>> elements,
            ClassLoader loader, Map<Integer, String> errorPages, Map<Class<?>, String> exceptionPages) {
        doWith(elements, (n) -> "error-page".equals(n), (o) -> {
            ErrorPageType errors = (ErrorPageType) o;
            if (errors.getErrorCode() != null) {
                Integer key = new Integer(errors.getErrorCode().getValue().toString());
                errorPages.put(key, parseLocation(errors.getLocation().getValue()));
            }
            else {
                Class<?> clz;
                try {
                    clz = loader.loadClass(errors.getExceptionType().getValue());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                exceptionPages.put(clz, parseLocation(errors.getLocation().getValue()));
            }
        });
    }

    private static void parseSessionConfig(Collection<JAXBElement<?>> elements, OurServletContext context) {
        doWith(elements, (n) -> "session-config".equals(n), (o) -> {
            SessionConfigType config = (SessionConfigType) o;
            if (config.getSessionTimeout() != null) {
                context.setMaxInactive(new Integer(config.getSessionTimeout().getValue().toString()));
            }
        });
    }

    /**
     * Parse a web-app definition file.
     *
     * @param url The URL pointing to the web-app definition
     * @param handler The servlet context to fill
     * @param welcomes The welcome file list, returned
     * @param errorPages The error page mapping, returned
     * @param exceptionPages The exception page mapping, returned
     * @throws Exception In case of errors
     */
    private static void parseFile(URL url, OurServletContext handler, Collection<String> welcomes,
            Map<Integer, String> errorPages, Map<Class<?>, String> exceptionPages) throws Exception {
        // We first do a transformation to allow acceptance of the old-style namespace for
        // web applications.
        String xslt = "/" + ServletContextParser.class.getPackage().getName().replace(".", "/") +
                "/translatens.xsl";
        Transformer trans = TransformerFactory.newInstance().newTransformer(
                new StreamSource(ServletContextParser.class.getClassLoader().getResourceAsStream(xslt)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        trans.transform(new StreamSource(url.openStream()), new StreamResult(out));
        // Do the JAXB parsing of the input.
        JAXBContext context = JAXBContext.newInstance(WebAppType.class);
        Unmarshaller unm = context.createUnmarshaller();
        JAXBElement<?> element = (JAXBElement<?>) unm.unmarshal(new ByteArrayInputStream(out.toByteArray()));
        WebAppType type = (WebAppType) element.getValue();
        // Process the elements.
        Collection<JAXBElement<?>> elements = type.getModuleNameOrDescriptionAndDisplayName();
        parseParameters(elements, handler);
        parseListeners(elements, handler);
        parseServlets(elements, handler);
        parseFilters(elements, handler);
        parseWelcomeFiles(elements, welcomes);
        parseErrorPages(elements, handler.getClassLoader(), errorPages, exceptionPages);
        parseSessionConfig(elements, handler);
    }

    /**
     * Parse a TLD, tag library descriptor from one of the META-INF directories of the
     * dependencies/local bundles.
     *
     * @param url The URL to parse
     * @param toReturn List where the found listener classes are written
     */
    private static void parseTLD(URL url, Collection<String> toReturn) {
        try {
            JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unm = context.createUnmarshaller();
            JAXBElement<?> element = (JAXBElement<?>) unm.unmarshal(url);
            TldTaglibType tld = (TldTaglibType) element.getValue();
            tld.getListener().forEach((l) -> toReturn.add(l.getListenerClass().getValue()));
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Load the listener classes from the tag library descriptors.
     *
     * @param bundle The bundle to parse (+ its dependencies)
     * @return A collection with listener class names
     */
    private static Collection<String> loadTLDListeners(Bundle bundle) {
        Collection<Bundle> bundles = DelegatingClassLoader.getDependencies(bundle);
        Collection<URL> urlsToParse = bundles.stream().
            map((b) -> b.findEntries("META-INF", "*.tld", true)).
            filter((e) -> e != null).
            map((e) -> Collections.list(e)).flatMap((c) -> c.stream()).collect(Collectors.toList());
        ArrayList<String> toReturn = new ArrayList<>();
        urlsToParse.forEach((u) -> parseTLD(u, toReturn));
        return toReturn;
    }
}
