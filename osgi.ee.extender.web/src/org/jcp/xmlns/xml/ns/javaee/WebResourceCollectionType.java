//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.12.13 at 07:41:28 PM CET 
//


package org.jcp.xmlns.xml.ns.javaee;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 * 
 *         The web-resource-collectionType is used to identify the
 *         resources and HTTP methods on those resources to which a
 *         security constraint applies. If no HTTP methods are specified,
 *         then the security constraint applies to all HTTP methods.
 *         If HTTP methods are specified by http-method-omission
 *         elements, the security constraint applies to all methods
 *         except those identified in the collection.
 *         http-method-omission and http-method elements are never
 *         mixed in the same collection. 
 *         
 *         Used in: security-constraint
 *         
 *       
 * 
 * <p>Java class for web-resource-collectionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="web-resource-collectionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="web-resource-name" type="{http://xmlns.jcp.org/xml/ns/javaee}string"/>
 *         &lt;element name="description" type="{http://xmlns.jcp.org/xml/ns/javaee}descriptionType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="url-pattern" type="{http://xmlns.jcp.org/xml/ns/javaee}url-patternType" maxOccurs="unbounded"/>
 *         &lt;choice minOccurs="0">
 *           &lt;element name="http-method" type="{http://xmlns.jcp.org/xml/ns/javaee}http-methodType" maxOccurs="unbounded"/>
 *           &lt;element name="http-method-omission" type="{http://xmlns.jcp.org/xml/ns/javaee}http-methodType" maxOccurs="unbounded"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "web-resource-collectionType", propOrder = {
    "webResourceName",
    "description",
    "urlPattern",
    "httpMethod",
    "httpMethodOmission"
})
public class WebResourceCollectionType {

    @XmlElement(name = "web-resource-name", required = true)
    protected org.jcp.xmlns.xml.ns.javaee.String webResourceName;
    protected List<DescriptionType> description;
    @XmlElement(name = "url-pattern", required = true)
    protected List<UrlPatternType> urlPattern;
    @XmlElement(name = "http-method")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected List<java.lang.String> httpMethod;
    @XmlElement(name = "http-method-omission")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected List<java.lang.String> httpMethodOmission;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected java.lang.String id;

    /**
     * Gets the value of the webResourceName property.
     * 
     * @return
     *     possible object is
     *     {@link org.jcp.xmlns.xml.ns.javaee.String }
     *     
     */
    public org.jcp.xmlns.xml.ns.javaee.String getWebResourceName() {
        return webResourceName;
    }

    /**
     * Sets the value of the webResourceName property.
     * 
     * @param value
     *     allowed object is
     *     {@link org.jcp.xmlns.xml.ns.javaee.String }
     *     
     */
    public void setWebResourceName(org.jcp.xmlns.xml.ns.javaee.String value) {
        this.webResourceName = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDescription().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptionType }
     * 
     * 
     */
    public List<DescriptionType> getDescription() {
        if (description == null) {
            description = new ArrayList<DescriptionType>();
        }
        return this.description;
    }

    /**
     * Gets the value of the urlPattern property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the urlPattern property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUrlPattern().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link UrlPatternType }
     * 
     * 
     */
    public List<UrlPatternType> getUrlPattern() {
        if (urlPattern == null) {
            urlPattern = new ArrayList<UrlPatternType>();
        }
        return this.urlPattern;
    }

    /**
     * Gets the value of the httpMethod property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the httpMethod property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHttpMethod().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link java.lang.String }
     * 
     * 
     */
    public List<java.lang.String> getHttpMethod() {
        if (httpMethod == null) {
            httpMethod = new ArrayList<java.lang.String>();
        }
        return this.httpMethod;
    }

    /**
     * Gets the value of the httpMethodOmission property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the httpMethodOmission property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHttpMethodOmission().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link java.lang.String }
     * 
     * 
     */
    public List<java.lang.String> getHttpMethodOmission() {
        if (httpMethodOmission == null) {
            httpMethodOmission = new ArrayList<java.lang.String>();
        }
        return this.httpMethodOmission;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setId(java.lang.String value) {
        this.id = value;
    }

}
