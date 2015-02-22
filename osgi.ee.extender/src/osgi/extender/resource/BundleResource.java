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

import java.io.InputStream;
import java.net.URL;

/**
 * A resource that is present in a bundle and can be retrieved and output to an user agent for
 * information.
 */
public interface BundleResource {
	/**
	 * Get the URL on which this resource can be found.
	 * 
	 * @return The URL on which this resource can be found
	 */
	public URL getURL();
	/**
	 * Get the stream that belongs to this resource.
	 * 
	 * @return The input stream
	 */
	public InputStream getInputStream();
}
