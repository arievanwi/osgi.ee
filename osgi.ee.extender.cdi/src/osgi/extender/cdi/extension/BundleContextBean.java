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
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.framework.BundleContext;

/**
 * Bean that provides a bundle context so it can be used as injection point for
 * injecting the context in other beans. Given the functionality of FrameworkUtil this is hardly
 * needed and can be solved in another way as well. However, this bean makes sure that the
 * bundle context of the bundle for which the bean manager is created is present.
 */
public class BundleContextBean implements Bean<BundleContext>{
    private BundleContext wrapped;
    
    public BundleContextBean(BundleContext c) {
        this.wrapped = c;
    }
    
    @Override
    public BundleContext create(CreationalContext<BundleContext> context) {
        return wrapped;
    }

    @Override
    public void destroy(BundleContext context,
            CreationalContext<BundleContext> c) {
    }

    @Override
    public String getName() {
        return "bundleContext";
    }

    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> set = new HashSet<>();
        Default def = new Default() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Default.class;
            }
        };
        set.add(def);
        return set;
    }

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
        types.add(BundleContext.class);
        return types;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Class<?> getBeanClass() {
        return BundleContext.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return new HashSet<>();
    }

    @Override
    public boolean isNullable() {
        return false;
    }
}
