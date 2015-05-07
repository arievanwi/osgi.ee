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
package osgi.extender.cdi.extension;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tracker of a specific service type and filter. Takes care of various types that
 * are injected as objects, collections or arrays of a specific type. The tracker tracks all
 * services matching the filter specified in the @ServiceReference indication and returns
 * proxies for the object and collection variant that match the service. At runtime a correctly
 * filled instance is returned.
 * 
 * @author Arie van Wijngaarden
 */
class Tracker<T> {
    private ServiceTracker<T, T> tracker;
    private Class<? extends T> trackedClass;
    private T proxy;
    private List<T> services;
    private long waitTime;
    
    /**
     * Create a tracker for a specific object/filter combination.
     * 
     * @param context The bundle context. Must be the context of the bundle we are extending
     * @param type The type of the service
     * @param subfilter The sub-filter for the service
     * @param waitTime The time, in ms, to wait for a service to become available
     * @throws InvalidSyntaxException In case the filter is incorrect
     */
    Tracker(BundleContext context, Class<? extends T> type, String subfilter, long waitTime) throws InvalidSyntaxException {
        // Get the wiring.
        BundleWiring wiring = context.getBundle().adapt(BundleWiring.class);
        // Get the filter from the class and the subfilter
        Filter filter = getFilter(type, subfilter);
        // Create a new proxy for this type. This proxy is used if a normal object is referenced.
        proxy = type.cast(Proxy.newProxyInstance(wiring.getClassLoader(), new Class<?>[]{type}, 
                new Wrapper<>(this::_getService)));
        // Construct the container for the tracked services that is i.e. returned by the collections variant.
        this.services = new CopyOnWriteArrayList<>();
        this.trackedClass = type;
        this.waitTime = waitTime;
        // And start tracking the services.
        tracker = new ServiceTracker<>(context, filter, new Customizer<>(context, this.services));
        tracker.open();
    }
    
    /**
     * Get a list with services. The list returned is backed by the actual list
     * and therefore always matches the actual services present in the OSGi environment
     * 
     * @return A list with services
     */
    List<T> getServiceList() {
        long toWait = waitTime;
        if (toWait < 0) {
            toWait = 0L;
        }
        synchronized (services) {
            if (services.size() < 1 && toWait > 0) {
                try {
                    services.wait(toWait);
                } catch (InterruptedException exc) {
                    Thread.currentThread().interrupt();
                }
            }
            return Collections.unmodifiableList(services);
        }
    }
    
    /**
     * Return the proxy that is the gateway to the first entry in the services
     * that are tracked by this tracker.
     * 
     * @return The proxy
     */
    T getService() {
        return proxy;
    }

    private T _getService() {
        long toWait = waitTime;
        if (toWait < 0) {
            toWait = 1000L;
        }
        return getService(toWait);
    }
    
    private T getService(long timeout) {
        try {
            return tracker.waitForService(timeout);
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
        }
        return trackedClass.cast(null);
    }
    
    /**
     * Get the service as a snapshot array.
     * 
     * @return An array with services. Note that this is, in contrast to the other
     * variants, a snapshot taking state of the status of the available services. 
     */
    T[] getServices() {
        List<T> serv = new ArrayList<>(getServiceList());
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(trackedClass, serv.size());
        return serv.toArray(array);
    }

    /**
     * Destroy this instance, closing the tracker.
     */
    public void destroy() {
        try {
            tracker.close();
        } catch (Exception exc) {}
    }
    
    /**
     * Construct a filter of a specific type with sub-filter.
     * 
     * @param type The main class that is tracked
     * @param subfilter The sub filter
     * @return An OSGi filter instance to be used for service tracking
     * @throws InvalidSyntaxException In case the sub-filter is incorrectly formatted
     */
    public static Filter getFilter(Class<?> type, String subfilter) throws InvalidSyntaxException {
        String filter = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
        if (subfilter != null && subfilter.trim().length() > 0) {
            filter = "(&" + filter + subfilter + ")";
        }
        return FrameworkUtil.createFilter(filter);
    }
}

/**
 * Wrapper that first executes a supplier to get the current state of a proxy
 * and then executes the method on that supplied value. 
 */
class Wrapper<T> implements InvocationHandler {
    private Supplier<T> supplier;

    Wrapper(Supplier<T> supplier) {
        this.supplier = supplier;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        T obj = supplier.get();
        if (obj == null) {
            throw new NullPointerException(
                    "(bugcheck): supplier for proxy returned null (service not present?). Cannot execute " + method);
        }
        try {
            return method.invoke(obj, args);
        } catch (InvocationTargetException exc) {
            throw exc.getCause();
        }
    }
}

/**
 * Customizer for handling services tracked. This to make sure that
 * we are always backed with a list of services.
 */
class Customizer<T> implements ServiceTrackerCustomizer<T, T> {
    private List<T> services;
    private BundleContext context;
    private Map<ServiceReference<T>, T> tracked = new TreeMap<>();
    
    Customizer(BundleContext context, List<T> services) {
        this.services = services;
        this.context = context;
    }
    
    @Override
    public T addingService(ServiceReference<T> ref) {
        T obj = context.getService(ref);
        if (obj != null) {
            synchronized (services) {
                tracked.put(ref,  obj);
                // Since tracked is sorted in ascending order, we need to revert the
                // order to match the OSGi default.
                services.clear();
                tracked.values().stream().forEach((s) -> services.add(0, s));
                services.notifyAll();
            }
        }
        return obj;
    }
    @Override
    public void modifiedService(ServiceReference<T> sr, T obj) {
    }
    @Override
    public void removedService(ServiceReference<T> sr, T obj) {
        context.ungetService(sr);
        synchronized (services) {
            tracked.remove(sr);
            services.remove(obj);
        }
    }
}