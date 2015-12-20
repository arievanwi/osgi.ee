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
 * Definition of a web context that should be created and started. Bundles can export services with this interface
 * to publish their context automatically. This interface is also published by the WAB extender on behalf of
 * a WAB (Web Application Bundle) when their manifest matches the requirements from the OSGi specification.
 */
public interface WebContextDefinition {
    /**
     * Get the context path of this web context. Is the first part of the URL that can be used to access the web
     * application.
     *
     * @return A string specifying the context path. Starts with a slash and does not end on a slash, like "/MyContext"
     */
    public String getContextPath();
    /**
     * Get the URL with the definition of the web application. The url should point to a valid web definition
     * file that follows the format of standard web applications (like normally present in WEB-INF/web.xml).
     *
     * @return The entry in the bundle registering the service that contains the definition. May be null.
     * The definition is loaded using Bundle.getEntry.
     */
    public String getDefinition();
    /**
     * Get the base directory for resources. This directory and everything below it will be served as resources
     * to the servlet context.
     *
     * @return The base location within the registering bundle that contains the resources. May be null
     */
    public String getResourceBase();
}
