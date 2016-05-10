/*
 * Copyright 2015, Imtech Traffic & Infra
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
package osgi.cdi.faces;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import osgi.extender.resource.BundleResource;
import osgi.extender.resource.BundleResourceProvider;

/**
 * Resource loader that loads resources from different bundles. It actually delegates the
 * resource logic to {@link BundleResourceProvider} services that are currently present in
 * the OSGi container. As a result, resources may be returned from another bundle (that
 * may by the way come and go). The functionality is therefore closely coupled to the
 * resource exporting functionality.
 *
 * @author Arie van Wijngaarden
 */
public class BundleResourceHandler extends ResourceHandlerWrapper {
    private ResourceHandler delegate;
    private ServiceTracker<BundleResourceProvider, BundleResourceProvider> tracker;

    /**
     * Construct a bundle resource handler that wraps another and performs lookups of resources
     * in the services that export bundle resources.
     *
     * @param wrapped The wrapped resource handler
     * @throws InvalidSyntaxException In case the filter in the context is invalid
     */
    public BundleResourceHandler(ResourceHandler wrapped) throws InvalidSyntaxException {
        delegate = wrapped;
        String filter = "(" + Constants.OBJECTCLASS + "=" + BundleResourceProvider.class.getName() + ")";
        String filt = FacesHelper.getFilter();
        if (filt != null) {
            filter = "(&" + filter + filt + ")";
        }
        BundleContext context = FacesHelper.context();
        tracker = new ServiceTracker<>(context, context.createFilter(filter), null);
        tracker.open();
    }

    @Override
    public ResourceHandler getWrapped() {
        return delegate;
    }

    /**
     * Get a resource from one of the found providers.
     *
     * @param path The path, basically the directory/lib where to find a resource
     * @param resource The resource name
     * @return A bundle resource, if found. Otherwise null
     */
    private BundleResource getResource(String path, String resource) {
        List<BundleResource> ress = tracker.getTracked().values().stream().
                map((rp) -> rp.getResource(path, resource)).
                filter((r) -> r != null).
                collect(Collectors.toList());
        // If found, return.
        if (ress.size() > 0) {
            return ress.get(0);
        }
        // If no further searching possible, end.
        if (path == null) {
            return null;
        }
        // Check if there is another grouping to be done.
        int index = path.indexOf("/");
        if (index < 0) {
            return getResource(null, path + "/" + resource);
        }
        return getResource(path.substring(0, index), path.substring(index + 1) + "/" + resource);
    }

    /**
     * Basic resource creation method. First tries to find the resource in the wrapped
     * resource handler and if not found there, locates the resource in one of the
     * {@link BundleResource} services that are tracked.
     */
    @Override
    public Resource createResource(String name, String lib, String type) {
        Resource resource = null;
        // Get the resource from the other bundles.
        BundleResource thisResource = getResource(lib, name);
        if (thisResource != null) {
            ServletContext context =
                    (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            // Find the faces registration.
            Optional<? extends ServletRegistration> registration =
                    context.getServletRegistrations().values().stream().
                    filter((sr) -> "javax.faces.webapp.FacesServlet".equals(sr.getClassName())).findFirst();
            // Check the mapping.
            String first = registration.get().getMappings().iterator().next();
            String p = null;
            // This is all according to the Faces specification.
            if (first.startsWith("*")) {
                // Extension mapping.
                p = context.getContextPath() + ResourceHandler.RESOURCE_IDENTIFIER + "/{}" + first.substring(1);
            }
            else if (first.endsWith("*")){
                // Must be prefix mapping.
                p = context.getContextPath() + first.substring(0, first.length() - 2) +
                        ResourceHandler.RESOURCE_IDENTIFIER + "/{}";
            }
            else {
                throw new RuntimeException("(bugcheck): cannot determine path for Faces servlet");
            }
            // Wrap it into a Resource object.
            resource = new OurResource(thisResource.getURL(), p);
            resource.setLibraryName(lib);
            resource.setResourceName(name);
        }
        else {
            resource = getWrapped().createResource(name, lib, type);
        }
        return resource;
    }

    @Override
    public Resource createResource(String name, String lib) {
        return createResource(name, lib, null);
    }

    @Override
    public Resource createResource(String name) {
        return createResource(name, null, null);
    }

    @Override
    public ViewResource createViewResource(FacesContext context, String resource) {
        BundleResource res = getResource(null, resource);
        ViewResource toReturn;
        if (res != null) {
            toReturn = new OurResource(res.getURL(), null);
        }
        else {
            toReturn = getWrapped().createViewResource(context, resource);
        }
        return toReturn;
    }

    @Override
    public boolean libraryExists(String exists) {
        return true;  // We don't check, just assume it is there.
    }
}

class OurResource extends Resource implements Serializable {
    private static final long serialVersionUID = 1L;
    private URL url;
    private String path;

    OurResource(URL u, String p) {
        url = u;
        path = p;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return url.openStream();
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }
    }

    @Override
    public String getRequestPath() {
        String query = "";
        String ln = getLibraryName();
        if (ln != null) {
            query += "?ln=" + ln;
        }
        return path.replace("{}", getResourceName()) + query;
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        Map<String, String> headers = new HashMap<>();
        return headers;
    }

    @Override
    public String getContentType() {
        return FacesContext.getCurrentInstance().getExternalContext().getMimeType(getResourceName());
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public boolean userAgentNeedsUpdate(FacesContext c) {
        return true;
    }
}