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
package osgi.extender.resource.impl;

import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import osgi.extender.resource.BundleResource;

/**
 * Our own implementation of a bundle resource. Simple wrapping.
 */
class OurBundleResource implements BundleResource {
	private Bundle bundle;
	private String path;
	
	OurBundleResource(Bundle bundle, String path) {
		this.bundle = bundle;
		this.path = path;
	}
	
	@Override
	public URL getURL() {
		return bundle.getEntry(path);
	}

	@Override
	public InputStream getInputStream() {
		return bundle.adapt(BundleWiring.class).getClassLoader().getResourceAsStream(path);
	}
}
