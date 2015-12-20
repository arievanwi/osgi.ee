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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Implementation of the HTTP session interface.
 */
@SuppressWarnings("deprecation")
class OurSession implements HttpSession, HttpSessionBindingListener {
    private Map<String, Object> attributes = new HashMap<>();
    private long created;
    private String id;
    private long lastAccessed;
    private int maxInactive;
    private OurServletContext context;
    private HttpSession parent;
    private static int sequence;

    OurSession(OurServletContext context, HttpSession parent) {
        this.context = context;
        this.parent = parent;
        synchronized (context) {
            created = lastAccessed = System.currentTimeMillis();
            id = new Long(created).toString() + sequence++;
        }
        int inact = context.getMaxInactive();
        if (inact <= 0) {
            inact = parent.getMaxInactiveInterval();
        }
        setMaxInactiveInterval(inact);
    }

    static String sessionKey(ServletContext context) {
        return HttpSession.class.getName() + ".session$$" + context;
    }

    @Override
    public Object getAttribute(String name) {
        checkValid();
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        checkValid();
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public long getCreationTime() {
        return created;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessed;
    }

    void setLastAccessedTime(long time) {
        lastAccessed = time;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactive;
    }

    @Override
    public ServletContext getServletContext() {
        checkValid();
        return context;
    }

    @Deprecated
    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Deprecated
    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Deprecated
    @Override
    public String[] getValueNames() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }

    @Override
    public void invalidate() {
        destroy();
        lastAccessed = 0;
    }

    @Override
    public boolean isNew() {
        return lastAccessed == created;
    }

    @Deprecated
    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    private void checkBindingListener(String name, Object obj, BiConsumer<HttpSessionBindingListener, HttpSessionBindingEvent> listener) {
        if (obj instanceof HttpSessionBindingListener) {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, obj);
            HttpSessionBindingListener l = (HttpSessionBindingListener) obj;
            listener.accept(l, event);
        }
    }

    private boolean unbindOriginal(String name) {
        Object original = attributes.get(name);
        if (original == null) {
            return false;
        }
        checkBindingListener(name, original, (l, e) -> l.valueUnbound(e));
        return true;
    }

    private void notifyListeners(String name, Object value, BiConsumer<HttpSessionAttributeListener, HttpSessionBindingEvent> listener) {
        HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, value);
        context.call(HttpSessionAttributeListener.class, (l) -> listener.accept(l, event));
    }

    private void _removeAttribute(String name) {
        unbindOriginal(name);
        attributes.remove(name);
        notifyListeners(name, null, (l, e) -> l.attributeRemoved(e));
    }

    @Override
    public void removeAttribute(String name) {
        checkValid();
        _removeAttribute(name);
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        // According to the specifications, a value of null should be handled as a remove.
        if (value == null) {
            removeAttribute(name);
            return;
        }
        checkValid();
        // See how to notify our listeners: as a replacement or an addition.
        BiConsumer<HttpSessionAttributeListener, HttpSessionBindingEvent> notifier = (l, e) -> l.attributeAdded(e);
        if (unbindOriginal(name)) {
            notifier = (l, e) -> l.attributeReplaced(e);
        }
        // Update the data.
        attributes.put(name, value);
        // And perform the notification.
        notifyListeners(name, value, notifier);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        maxInactive = interval < 0 ? 3000 : interval;
        if (parent.getMaxInactiveInterval() < maxInactive) {
            parent.setMaxInactiveInterval(maxInactive);
        }
    }

    boolean isValid() {
        return lastAccessed >= created && lastAccessed + maxInactive * 60 * 1000 > System.currentTimeMillis();
    }

    private void checkValid() {
        if (!isValid()) {
            throw new IllegalStateException("session is invalid");
        }
    }

    private void destroy() {
        new ArrayList<>(attributes.keySet()).forEach((k) -> _removeAttribute(k));
        parent.removeAttribute(sessionKey(context));
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        context.call(HttpSessionListener.class, (l) -> l.sessionCreated(new HttpSessionEvent(this)));
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        context.call(HttpSessionListener.class, (l) -> l.sessionDestroyed(new HttpSessionEvent(this)));
    }
}