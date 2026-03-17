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
package osgi.extender.jpa.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Persistence unit definition. From a standard schema. We don't care about the version, just
 * use some transformation to get the data from a standard persistence.xml.
 */
class PersistenceUnitDefinition {
    public String version;
    public String name;
    public String transactionType;
    public String description;
    public String provider;
    public String nonJtaDs;
    public String jtaDs;
    public List<String> mappingFiles;
    public List<String> jarFiles;
    public List<String> classes;
    public boolean excludeUnlisted = true;
    public String cachingType;
    public String validationMode;
    public List<Property> properties;

    private static Result getTransformer(Result result) throws Exception {
        SAXTransformerFactory fact = (SAXTransformerFactory) TransformerFactory.newInstance();
        String xsl = "/" + PersistenceUnitDefinition.class.getPackage().getName().replace(
                ".", "/") + "/persistence.xsl";
        TransformerHandler handler;
        try (InputStream xsls = PersistenceUnitDefinition.class.getClassLoader().getResourceAsStream(xsl)) {
            // Create the final pipeline.
            handler = fact.newTransformerHandler(new StreamSource(xsls));
            handler.setResult(result);
            String preTransform = System.getProperty("osgi.jpa.transform");
            if (preTransform != null) {
                // Construct the transformation pipeline.
                try (FileInputStream pf = new FileInputStream(preTransform)) {
                    TransformerHandler preTransformer = fact.newTransformerHandler(new StreamSource(pf));
                    preTransformer.setResult(new SAXResult(handler));
                    handler = preTransformer;
                }
            }
        } 
        return new SAXResult(handler);
    }
    
    static List<PersistenceUnitDefinition> fromFile(InputStream in) throws Exception {
        // Prepare to get the result.
    	DOMResult result = new DOMResult();
        // Construct the non-transforming transformer of the input XML stream.
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        // Do the transformation.
        trans.transform(new StreamSource(in), getTransformer(result));
        // And de-serialize it.
        List<PersistenceUnitDefinition> list = new ArrayList<>();
        NodeList nodes = result.getNode().getFirstChild().getChildNodes();
        for (int cnt = 0; cnt < nodes.getLength(); cnt++) {
        	Node node = nodes.item(cnt);
        	if (node.getNodeType() != Node.ELEMENT_NODE) continue;
        	if ("object".equals(node.getNodeName()) && 
        			PersistenceUnitDefinition.class.getName().equals(node.getAttributes().getNamedItem("class").getNodeValue())) {
            	// Now process the elements in the node.
	        	PersistenceUnitDefinition def = new PersistenceUnitDefinition();
	        	list.add(def);
	        	NodeList contents = node.getChildNodes();
	        	for (int fieldIndex = 0; fieldIndex < contents.getLength(); fieldIndex++) {
	        		Node n = contents.item(fieldIndex);
	        		Field field = PersistenceUnitDefinition.class.getField(n.getNodeName());
	        		if (List.class.isAssignableFrom(field.getType())) {
	        			List<Object> l = new ArrayList<>();
	        			field.set(def, l);
	        			// Check the type.
	        			ParameterizedType pt = (ParameterizedType) field.getGenericType();
	        			Class<?> clz = (Class<?>) pt.getActualTypeArguments()[0];
        				NodeList s = n.getChildNodes();
	        			if (String.class.isAssignableFrom(clz)) {
	        				for (int sub = 0; sub < s.getLength(); sub++) {
	        					Node sn = s.item(sub);
	        					l.add(sn.getTextContent());
	        				}
	        			}
	        			else {
	        				// Must be the Properties.
	        				for (int sub = 0; sub < s.getLength(); sub++) {
	        					Property p = new Property();
	        					l.add(p);
	        					Node sn = s.item(sub);
	        					NodeList ssnl = sn.getChildNodes();
	        					for (int ssub = 0; ssub < ssnl.getLength(); ssub++) {
	        						Node ssn = ssnl.item(ssub);
	        						Field f = Property.class.getField(ssn.getNodeName());
	        						f.set(p, ssn.getTextContent());
	        					}
	        				}
	        			}
	        		}
	        		else {
	        			// Must be a string or a boolean.
	        			if (String.class.isAssignableFrom(field.getType())) {
	        				String value = n.getTextContent();
	        				field.set(def, value);
	        			}
	        			else {
	        				// Must be a boolean.
	        				boolean value = Boolean.parseBoolean(n.getTextContent());
	        				field.set(def, value);
	        			}
	        		}
	        	}
        	}
        }
        return list;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        try {
            PersistenceUnitDefinition def = (PersistenceUnitDefinition) obj;
            return def.name.equals(this.name);
        } catch (Exception exc) {
            return false;
        }
    }
}

class Property {
	public String key;
	public String value;
}
