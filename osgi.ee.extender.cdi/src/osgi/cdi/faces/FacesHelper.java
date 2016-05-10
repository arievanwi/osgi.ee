/*
 * Copyright 2016, aVineas IT Consulting
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
package osgi.cdi.faces;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.osgi.framework.BundleContext;

/**
 * Some common helper functionalities to perform OSGi related actions on faces information.
 *
 * @author Arie van Wijngaarden
 */
class FacesHelper {
    /**
     * Get the bundle context. It is retrieved from the servlet attribute as per OSGi web specifiction.
     *
     * @return The bundle context
     */
    static BundleContext context() {
        FacesContext context = FacesContext.getCurrentInstance();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
        final BundleContext bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        return bundleContext;
    }

    /**
     * Get the filter specification as present in the init parameter.
     *
     * @return The filter specification. May be null
     */
    static String getFilter() {
        return FacesContext.getCurrentInstance().getExternalContext().getInitParameter("osgi.extender.cdi.faces.filter");
    }
}