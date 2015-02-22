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
package osgi.extender.resource;

import java.util.Collection;

/**
 * Provider of bundle resources. Provides the opportunity to load resources from a bundle
 * for faces handling, although not really limited to this.
 */
public interface BundleResourceProvider {
	public static String DEFAULT = null;
	/**
	 * Provide the resources that are found at the specified logical directory. It is actually a wildcard search
	 * on that directory to find sub-entries that may be in it.
	 *
	 * @param dir The logical directory. Specifies the location, etc. of a library that may be defined
	 * for the bundle. Specify null (DEFAULT) for getting the default resources from the default directory
	 */
	public Collection<BundleResource> getResources(String directory);
	/**
	 * Get a bundle resource by name. This is a one-fetch operation to get one resource back.
	 * 
	 * @param directory The logical directory on which the path resides
	 * @param path The sub-path within the directory
	 * @return The bundle resource or null if not found
	 */
	public BundleResource getResource(String directory, String path);
}
