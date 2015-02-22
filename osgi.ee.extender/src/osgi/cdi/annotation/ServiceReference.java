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
package osgi.cdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * OSGi service reference annotation. Annotation, if set as qualifier on an injection point,
 * indicates that the field, parameter or setter method should be injected with a OSGi service.
 * May refer to either (1) a normal object, (2) a collection of objects or (3) an array of
 * objects. The first 2 variants are backed via proxies, the 3rd variant is a snapshot taken
 * from the list of services at the time of bean instantiation.
 * 
 * @author Arie van Wijngaarden
 */
@Qualifier
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceReference {
    /** 
     * The OSGi filter to apply to the lookup via a ServiceTracker. 
     * Normal OSGi LDAP specification. The objectClass part of the filter is automatically
     * determined based on the type of the injection point.
     */ 
    String filter() default "";
}
