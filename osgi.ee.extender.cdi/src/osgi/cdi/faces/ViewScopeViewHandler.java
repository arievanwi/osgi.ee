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
package osgi.cdi.faces;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import osgi.extender.cdi.scopes.ScopeListener;

/**
 * View handler that takes care of tracking new view scopes. This in case a new view is started with
 * the same URL.
 */
public class ViewScopeViewHandler extends ViewHandlerWrapper {
    private static final String RESTORING = ViewScopeViewHandler.class.getName() + ".restoring";
    private ViewHandler wrapped;

    public ViewScopeViewHandler(ViewHandler h) {
        wrapped = h;
    }

    @Override
    public UIViewRoot createView(FacesContext fc, String page) {
        // If the view is not being restored, but completely new, destroy the view scope.
        HttpServletRequest request = (HttpServletRequest) fc.getExternalContext().getRequest();
        Boolean restoring = (Boolean) request.getAttribute(RESTORING);
        if (restoring == null || restoring == false) {
            ScopeListener listener = (ScopeListener) request.getAttribute(ScopeListener.SCOPELISTENER);
            if (listener != null) {
                listener.setViewScope(request, true);
            }
        }
        return super.createView(fc, page);
    }

    @Override
    public ViewHandler getWrapped() {
        return wrapped;
    }

    @Override
    public UIViewRoot restoreView(FacesContext fc, String page) {
        // Mark the view as being restored.
        HttpServletRequest request = (HttpServletRequest) fc.getExternalContext().getRequest();
        request.setAttribute(RESTORING, true);
        return super.restoreView(fc, page);
    }
}
