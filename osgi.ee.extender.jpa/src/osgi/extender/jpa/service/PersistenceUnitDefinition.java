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
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Persistence unit definition. From a standard schema. We don't care about the version, just
 * use some transformation to get the data from a standard persistence.xml.
 */
class PersistenceUnitDefinition {
    String version;
    String name;
    String transactionType;
    String description;
    String provider;
    String nonJtaDs;
    String jtaDs;
    List<String> mappingFiles;
    List<String> jarFiles;
    List<String> classes;
    boolean excludeUnlisted = true;
    String cachingType;
    String validationMode;
    Map<String, String> properties;

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
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        // Construct the non-transforming transformer of the input XML stream.
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        // Do the transformation.
        trans.transform(new StreamSource(in), getTransformer(result));
        // And de-serialize it.
        XStream stream = new XStream(new DomDriver());
        @SuppressWarnings("unchecked")
        List<PersistenceUnitDefinition> list = 
            (List<PersistenceUnitDefinition>) stream.fromXML(writer.getBuffer().toString());
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
