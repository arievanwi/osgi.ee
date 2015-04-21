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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;

import osgi.cdi.annotation.ComponentScoped;
import osgi.cdi.annotation.Service;
import osgi.cdi.annotation.ServiceReference;

/**
 * Standard CDI extension. Takes care of handling the service logic related to OSGi environments, meaning that
 * it takes care of service registration and service reference injection handling that occur
 * in CDI contexts.
 * 
 * @author Arie van Wijngaarden
 */
public class ServiceExtension implements Extension {
    private BundleContext context;
    private Map<String, Tracker<?>> trackers;                     // Trackers for the imported services
    private Set<TrackerBean> beans;                               // Beans created to match the @ServiceReference-s
    private Map<Bean<?>, ServiceRegistration<?>> exportedBeans;   // The @Service annotated beans with their registration
    
    /**
     * Construct the extension.
     * 
     * @param context The bundle context. Used for declaring services and looking up services
     * and must be the bundle we are extending
     */
    public ServiceExtension(BundleContext context) {
        this.context = context;
        this.trackers = new HashMap<>();
        this.beans = new HashSet<>();
        this.exportedBeans = new HashMap<>();
    }
    
    /**
     * Event generated by the container to process an injection point. Is called for every point defined in the
     * container. During this phase, we check for any @ServiceReference qualifiers and create a bean for it.
     * That bean is later added during another phase of the container construction. 
     * 
     * @param point The injection point event
     */
    public <T, X> void processInjectionPoints(@Observes ProcessInjectionPoint<T, X> event) {
        TrackerBean bean = processInjectionPoint(event.getInjectionPoint(), (e) -> event.addDefinitionError(e));
        if (bean != null) {
            beans.add(bean);
        }
    }
    
    /**
     * Processing method for every injection point. Depending on the definition we will mark some injection
     * points to be related to a bean that will be added to the container that will resolve this one. This is
     * done if the injection point is marked as {@link ServiceReference}. In that case we determine the filter and
     * define a bean for it later.
     * 
     * @param point The injection point to process
     * @param errorAdder Method used to add an error
     */
    private TrackerBean processInjectionPoint(InjectionPoint point, Consumer<Throwable> errorAdder) {
        List<ServiceReference> qual = point.getQualifiers().stream().
                filter((q) -> q.annotationType().equals(ServiceReference.class)).
                map((s) -> ServiceReference.class.cast(s)).collect(Collectors.toList());
        // See if it has exactly one service annotation.
        if (qual.size() == 0) return null;
        if (qual.size() > 1) {
            errorAdder.accept(new Exception("only one @Service annotation allowed per injection point"));
            return null;
        }
        // Marked as a service.
        ServiceReference service = qual.get(0);
        Function<Tracker<?>, Object> mapper = null;
        Class<?> toTrack = null;
        Type type = point.getType();
        if (type instanceof Class) {
            // Easy one: just track the class.
            // Can be an array though.
            Class<?> clz = (Class<?>) type;
            if (clz.isArray()) {
                // Check the bean scope. Arrays cannot be injected into global scopes.
                Class<? extends Annotation> scope = point.getBean().getScope();
                if (!scope.equals(Dependent.class) && !scope.equals(RequestScoped.class)) {
                    errorAdder.accept(new Exception("array type @ServiceReference-s cannot be "
                            + "injected in global scopes. Use a collection"));
                }
                else {
                    toTrack = clz.getComponentType();
                    mapper = (a) -> a.getServices();
                }
            }
            else {
                toTrack = clz;
                mapper = (a) -> a.getService();
            }
        }
        else if (type instanceof ParameterizedType) {
            // It is a parameterized type. Check the raw type.
            ParameterizedType t = (ParameterizedType) type;
            // Must be a collection class. Check this.
            if (t.getRawType() instanceof Class) {
                Class<?> container = (Class<?>) t.getRawType();
                if (container.isAssignableFrom(List.class)) {
                    // We can use it as a consumer of a list. Locate the first raw type at any depth.
                    Type firstT = t.getActualTypeArguments()[0];
                    Class<?> firstType = null;
                    if (firstT instanceof Class) {
                        firstType = (Class<?>) firstT;
                    }
                    else if (firstT instanceof ParameterizedType) {
                        ParameterizedType thisT = (ParameterizedType) firstT;
                        firstType = (Class<?>) thisT.getRawType();
                    }
                    else {
                        errorAdder.accept(new Exception("invalid List contents for service reference. Don't understand " + firstT));
                    }
                    if (firstType != null) {
                        // Use a tracker for this type.
                        toTrack = firstType;
                        mapper = (a) -> a.getServiceList();
                    }
                }
                else {
                    errorAdder.accept(new Exception("only List<?> or its supertypes may be used for "
                            + "parameterized @ServiceReference annotations"));
                }
            }
            else {
                errorAdder.accept(new Exception("don't understand " + t.getRawType() + 
                        "as parameterized @ServiceReference type"));
            }
        }
        else {
            errorAdder.accept(new Exception("don't understand " + type + " as @ServiceReference type"));
        }
        if (toTrack == null) return null;
        // Now handle the tracking.
        String subfilter = service.filter();
        long waitTime = service.timeout();
        Filter filter;
        try {
            filter = Tracker.getFilter(toTrack, subfilter);
        } catch (Exception exc) {
            errorAdder.accept(new Exception("filter " + subfilter + " is an invalid service filter"));
            return null;
        }
        // Get the tracker for this filter
        Tracker<?> tracker = trackers.get(filter.toString());
        if (tracker == null) {
            try {
                tracker = new Tracker<Object>(this.context, toTrack, subfilter, waitTime);
                trackers.put(filter.toString(), tracker);
            } catch (Exception exc) {
                errorAdder.accept(exc);
                return null;
            }
        }
        // OK. Create the bean.
        TrackerBean bean = new TrackerBean(point.getQualifiers(), point.getType(), tracker, mapper);
        return bean;
    }

    /**
     * Process a bean. Checks the bean for @Service annotation and if it is found, verifies
     * the bean scope and marks it for export as service at a later stage.
     * 
     * @param event The process bean event. See CDI specification
     */
    public <T> void processBean(@Observes ProcessBean<T> event) {
        Bean<T> bean = event.getBean();
        Service service = BeanExporter.getServiceDefinition(bean);
        if (service == null) return;
        // Check the scope. Must be global.
        Class<? extends Annotation> scope = bean.getScope();
        if (!scope.equals(ComponentScoped.class) && !scope.equals(ApplicationScoped.class)) { 
            // Set an error.
            event.addDefinitionError(new Exception("beans annotated with @Service must have a global scope"));
            return;
        }
        // Mark the bean as exportable.
        exportedBeans.put(bean, null);
    }

    /**
     * Handling of the after bean discovery event as fired by the bean manager. The handling creates
     * contexts for the session and request scopes and registers the annotations for injection of
     * services (and registration of them).
     * 
     * @param event The event that can be used for the actions
     */
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        // Add the beans that satisfy the @ServiceReference injection points
        this.beans.stream().forEach((b) -> event.addBean(b));
    }
    
    /**
     * Method that must be called after all events are generated and the CDI context is ready to
     * start processing beans. It registers the global scope service beans. This is done here,
     * because only now we know that the container is ready to start processing requests.
     * 
     * @param manager The bean manager
     */
    @SuppressWarnings("unchecked")
    public <T> void finish(@Observes AfterDeploymentValidation val, BeanManager manager) {
        exportedBeans.entrySet().stream().forEach((e) -> {
            Bean<T> bean = (Bean<T>) e.getKey();
            CreationalContext<T> cc = manager.createCreationalContext(bean);
            Object instance = bean.create(cc);
            e.setValue(BeanExporter.registerService(context, bean, instance));
        });
    }
    
    /**
     * Destroy this extension. Removes all services.
     */
    public void destroy(@Observes BeforeShutdown shut) {
        this.exportedBeans.values().stream().
            forEach((s) ->  {
                try {
                    s.unregister();
                } catch (Exception exc) {}
            });
        this.trackers.values().forEach((t) -> t.destroy());
    }
}