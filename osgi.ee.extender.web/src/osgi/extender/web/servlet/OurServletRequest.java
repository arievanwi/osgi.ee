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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * Wrapper that does some re-definitions because of the context changes done by our
 * servlet handler.
 */
class OurServletRequest extends HttpServletRequestWrapper {
    private OurServletContext context;
    private String pathInfo;
    private String servletPath;

    public OurServletRequest(HttpServletRequest request, OurServletContext context, String servletPath, String pathInfo) {
        super(request);
        this.context = context;
        this.pathInfo = pathInfo;
        this.servletPath = servletPath;
    }

    @Override
    public String getContextPath() {
        return context.getContextPath();
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    private HttpServletRequest request() {
        return (HttpServletRequest) getRequest();
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }

    @Override
    public HttpSession getSession(boolean create) {
        HttpSession session = request().getSession(create);
        if (session == null) {
            return null;
        }
        // Check if we have our session there.
        String key = OurSession.sessionKey(context);
        OurSession ours = (OurSession) session.getAttribute(key);
        if (ours == null || !ours.isValid()) {
            if (create) {
                ours = new OurSession(context, session);
                session.setAttribute(key, ours);
            }
        }
        else {
            ours.setLastAccessedTime(System.currentTimeMillis());
        }
        return ours;
    }
}
