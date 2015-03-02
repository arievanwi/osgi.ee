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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Tracker bean. A bean definition that tracks services and allows for
 * injection into injection points. As a result, the container is satisfied
 * with matches of those points.
 * 
 * @author Arie van Wijngaarden
 */
public class TrackerBean implements Bean<Object> {
    private Set<Annotation> qualifiers;
    private Type type;
    private Tracker<?> tracker;
    private Function<Tracker<?>, Object> function;
    
    /**
     * Tracker bean constructor. Constructor for one type (one of object, collection or array).
     * 
     * @param qualifiers The qualifiers on the injection point. Is one-to-one copied to the bean to
     * make sure they match
     * @param type The type of the reference. Again from the injection point to make a perfect match
     * @param tracker The object tracker to use as service tracker
     * @param function The function to execute on the tracker to get the object to inject
     */
    TrackerBean(Set<Annotation> qualifiers, Type type, Tracker<?> tracker, Function<Tracker<?>, Object> function) {
        this.qualifiers = qualifiers;
        this.tracker = tracker;
        this.type = type;
        this.function = function;
    }
    
    @Override
    public Object create(CreationalContext<Object> context) {
        Object obj = function.apply(tracker);
        return obj;
    }

    @Override
    public void destroy(Object instance, CreationalContext<Object> context) {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /**
     * We always use dependent scope, because otherwise the bean manager wants to construct
     * the bean itself (and thus requires a no-arg constructor). It doesn't make much difference
     * since all the overhead is in the tracker anyway.
     */
    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return new HashSet<>();
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> types = new HashSet<>();
        types.add(type);
        return types;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Class<?> getBeanClass() {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            return (Class<?>) ptype.getRawType();
        }
        return (Class<?>) type;    
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return new HashSet<>();
    }

    @Override
    @Deprecated
    public boolean isNullable() {
        return false;
    }

    @Override
    public int hashCode() {
        return qualifiers.hashCode();
    }

    /**
     * Equals is implemented because in the extension a set is maintained. Just in 
     * case the same type of injection point is used in various beans. We only need one
     * bean to satisfy them all.
     */
    @Override
    public boolean equals(Object obj) {
        try {
            TrackerBean that = (TrackerBean) obj;
            if (!that.qualifiers.equals(qualifiers)) return false;
            if (!that.type.equals(type)) return false;
            return true;
        } catch (Exception exc) {
            return false;
        }
    }
}
