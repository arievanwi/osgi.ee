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

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Servlet response wrapper. Handles the error pages because of error setting, etc.
 */
public class OurServletResponse extends HttpServletResponseWrapper {
    private Map<String, String> errors;
    private String context;

    public OurServletResponse(HttpServletResponse response, String context, Map<String, String> errors) {
        super(response);
        this.errors = errors;
        this.context = context;
    }

    private String codeToUrl(int code) {
        String page = errors.get(new Integer(code).toString());
        if (page != null) {
            try {
                sendRedirect(context + page);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        return page;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (codeToUrl(sc) == null) {
            super.sendError(sc, msg);
        }
    }

    @Override
    public void sendError(int sc) throws IOException {
        if (codeToUrl(sc) == null) {
            super.sendError(sc);
        }
    }

    @Override
    public void setStatus(int sc) {
        if (codeToUrl(sc) == null) {
            super.setStatus(sc);
        }
    }
}
