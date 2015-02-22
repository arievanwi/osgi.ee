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
package osgi.extender.cdi.extension.context;

import java.lang.annotation.Annotation;

/**
 * Simple context definition that is global and contains only one cache, identified
 * by the instance itself.
 */
public class BasicContext extends AbstractContext {

    public BasicContext(Class<? extends Annotation> scope, ContextBeansListener listener) {
        super(scope, listener);
        add(this);
    }

    @Override
    protected ContextBeansHolder getCache() {
        return getCache(this);
    }
}
