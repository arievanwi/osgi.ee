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
package osgi.jta.faces;

import java.util.Iterator;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.servlet.ServletContext;
import javax.transaction.TransactionManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Exception handler factory that takes care of catching any exceptions and rolling back
 * the state of the transaction, if any.
 */
public class JtaExceptionHandlerFactory extends ExceptionHandlerFactory {
    private ExceptionHandlerFactory parent;
    private ServiceTracker<TransactionManager, TransactionManager> tracker;
    
    public JtaExceptionHandlerFactory(ExceptionHandlerFactory p) {
        this.parent = p;
    }

    @Override
    public synchronized ExceptionHandler getExceptionHandler() {
        if (tracker == null) {
            ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            BundleContext ctx = (BundleContext) context.getAttribute("osgi-bundlecontext");
            if (ctx == null) {
                ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
            }
            tracker = new ServiceTracker<>(ctx, TransactionManager.class, null);
            tracker.open();
        }
        return new JtaExceptionHandler(parent.getExceptionHandler(), tracker);
    }
}

class JtaExceptionHandler extends ExceptionHandlerWrapper {
    private ExceptionHandler delegate;
    private ServiceTracker<TransactionManager, TransactionManager> tracker;

    public JtaExceptionHandler(ExceptionHandler parent, ServiceTracker<TransactionManager, TransactionManager> t) {
        this.delegate = parent;
        this.tracker = t;
    }
    
    @Override
    public void handle() {
        Iterator<ExceptionQueuedEvent> it = getUnhandledExceptionQueuedEvents().iterator();
        if (it.hasNext()) {
            TransactionManager manager = tracker.getService();
            if (manager != null) {
                try {
                    manager.setRollbackOnly();
                } catch (Exception exc) {}
            }
        }
        getWrapped().handle();
    }

    @Override
    public ExceptionHandler getWrapped() {
        return delegate;
    }
}
