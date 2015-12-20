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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;

import osgi.extender.web.servlet.support.DynamicFRegistration;
import osgi.extender.web.servlet.support.DynamicSRegistration;

/**
 * Calculator and cache for the various chains of execution of servlet requests.
 */
class ChainCalculator {
    /**
     * Determine the filter chain for a specific path given the definition of servlets and filters. The routine
     * first determines the servlet for which the path applies and if found, determines the filters that need to
     * be used. The complete chain is returned as a standard FilterChain.
     *
     * @param filters The filters that are defined
     * @param servlets The servlets that are defined
     * @param path The path, meaning the URI without the context, that needs to be matched
     * @param servletPath The servlet path as determined, returned
     * @return The filter chain to execute on the incoming request/response
     */
    static FilterChain getChain(Map<String, DynamicFRegistration> filters, Map<String, DynamicSRegistration> servlets,
            String path, StringBuffer servletPath) {
        // Determine the best matching servlet.
        int bestMatchLength = -1;
        String bestServlet = null;
        for (Map.Entry<String, DynamicSRegistration> entry : servlets.entrySet()) {
            DynamicSRegistration reg = entry.getValue();
            for (String mapping : reg.getMappings()) {
                int match = matchLength(path, mapping);
                if (match > bestMatchLength) {
                    bestMatchLength = match;
                    bestServlet = entry.getKey();
                }
            }
        }
        if (bestServlet == null) {
            return null;
        }
        // Determine the filters.
        final String bestServletName = bestServlet;
        List<DynamicFRegistration> filtersToUse =
                filters.values().stream().filter((f) -> {
                    if (f.getServletNameMappings().contains(bestServletName)) {
                        return true;
                    }
                    for (String pm : f.getUrlPatternMappings()) {
                        if (matchLength(path, pm) >= 0) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
        // Now we got zero or more filters and one servlet. Create a chain from it.
        Servlet servlet = servlets.get(bestServlet).getObject();
        FilterChain chain = (request, response) -> servlet.service(request, response);
        // Now wrap the chain with the filters, in reverse order.
        Collections.reverse(filtersToUse);
        for (DynamicFRegistration fr : filtersToUse) {
            final FilterChain original = chain;
            final Filter f = fr.getObject();
            chain = (r, rr) -> f.doFilter(r, rr, original);
        }
        // Return the servlet path.
        int servletPathLength = bestMatchLength;
        if (bestMatchLength > 0) {
            servletPathLength--;
        }
        servletPath.append(path.substring(0, servletPathLength));
        return chain;
    }

    /**
     * Check the match length of a specific mapping. Routine calculates the match length on the various types of path/url mappings
     * that are possible for servlets (and filters). It determines the type of mapping used and determines the match length
     * from it: for path mapping the length of the path, for extension mapping the length is set to 0.
     *
     * @param path The path to check
     * @param mapping The mapping to check against
     * @return The length of the match, -1 on no match at all
     */
    private static int matchLength(String path, String mapping) {
        int length = -1;
        if (mapping.startsWith("*")) {
            // Extension matching.
            if (path.endsWith(mapping.substring(1))) {
                length = 0;
            }
        }
        else if (mapping.endsWith("*")) {
            // Path matching.
            if (path.startsWith(mapping.substring(0, mapping.length() - 1))) {
                length = mapping.length() - 1;
            }
        }
        else {
            // Exact matching.
            if (path.equals(mapping)) {
                length = mapping.length();
            }
        }
        return length;
    }
}