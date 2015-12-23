//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.12.23 at 06:46:33 PM CET 
//


package com.sun.java.xml.ns.javaee;

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
 * 	The taglib tag is the document root, it defines:
 * 
 * 	description     a simple string describing the "use" of this
 * 			taglib, should be user discernable
 * 
 * 	display-name    the display-name element contains a
 * 			short name that is intended to be displayed
 * 			by tools
 * 
 * 	icon            optional icon that can be used by tools
 * 
 * 	tlib-version    the version of the tag library implementation
 * 
 * 	short-name      a simple default short name that could be
 * 			used by a JSP authoring tool to create
 * 			names with a mnemonic value; for example,
 * 			the it may be used as the prefered prefix
 * 			value in taglib directives
 * 
 * 	uri             a uri uniquely identifying this taglib
 * 
 * 	validator       optional TagLibraryValidator information
 * 
 * 	listener        optional event listener specification
 * 
 * 	tag             tags in this tag library
 * 
 * 	tag-file        tag files in this tag library
 * 
 * 	function        zero or more EL functions defined in this
 * 			tag library
 * 
 * 	taglib-extension zero or more extensions that provide extra
 * 			information about this taglib, for tool
 * 			consumption
 * 
 *       
 * 
 * <p>Java class for tldTaglibType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tldTaglibType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://java.sun.com/xml/ns/javaee}descriptionGroup"/>
 *         &lt;element name="tlib-version" type="{http://java.sun.com/xml/ns/javaee}dewey-versionType"/>
 *         &lt;element name="short-name" type="{http://java.sun.com/xml/ns/javaee}tld-canonical-nameType"/>
 *         &lt;element name="uri" type="{http://java.sun.com/xml/ns/javaee}xsdAnyURIType" minOccurs="0"/>
 *         &lt;element name="validator" type="{http://java.sun.com/xml/ns/javaee}validatorType" minOccurs="0"/>
 *         &lt;element name="listener" type="{http://java.sun.com/xml/ns/javaee}listenerType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="tag" type="{http://java.sun.com/xml/ns/javaee}tagType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="tag-file" type="{http://java.sun.com/xml/ns/javaee}tagFileType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="function" type="{http://java.sun.com/xml/ns/javaee}functionType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="taglib-extension" type="{http://java.sun.com/xml/ns/javaee}tld-extensionType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="version" use="required" type="{http://java.sun.com/xml/ns/javaee}dewey-versionType" fixed="2.1" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tldTaglibType", propOrder = {
    "description",
    "displayName",
    "icon",
    "tlibVersion",
    "shortName",
    "uri",
    "validator",
    "listener",
    "tag",
    "tagFile",
    "function",
    "taglibExtension"
})
public class TldTaglibType {

    protected List<DescriptionType> description;
    @XmlElement(name = "display-name")
    protected List<DisplayNameType> displayName;
    protected List<IconType> icon;
    @XmlElement(name = "tlib-version", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected java.lang.String tlibVersion;
    @XmlElement(name = "short-name", required = true)
    protected TldCanonicalNameType shortName;
    protected XsdAnyURIType uri;
    protected ValidatorType validator;
    protected List<ListenerType> listener;
    protected List<TagType> tag;
    @XmlElement(name = "tag-file")
    protected List<TagFileType> tagFile;
    protected List<FunctionType> function;
    @XmlElement(name = "taglib-extension")
    protected List<TldExtensionType> taglibExtension;
    @XmlAttribute(name = "version", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected java.lang.String version;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected java.lang.String id;

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
     * Gets the value of the displayName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the displayName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDisplayName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DisplayNameType }
     * 
     * 
     */
    public List<DisplayNameType> getDisplayName() {
        if (displayName == null) {
            displayName = new ArrayList<DisplayNameType>();
        }
        return this.displayName;
    }

    /**
     * Gets the value of the icon property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the icon property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIcon().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IconType }
     * 
     * 
     */
    public List<IconType> getIcon() {
        if (icon == null) {
            icon = new ArrayList<IconType>();
        }
        return this.icon;
    }

    /**
     * Gets the value of the tlibVersion property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getTlibVersion() {
        return tlibVersion;
    }

    /**
     * Sets the value of the tlibVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setTlibVersion(java.lang.String value) {
        this.tlibVersion = value;
    }

    /**
     * Gets the value of the shortName property.
     * 
     * @return
     *     possible object is
     *     {@link TldCanonicalNameType }
     *     
     */
    public TldCanonicalNameType getShortName() {
        return shortName;
    }

    /**
     * Sets the value of the shortName property.
     * 
     * @param value
     *     allowed object is
     *     {@link TldCanonicalNameType }
     *     
     */
    public void setShortName(TldCanonicalNameType value) {
        this.shortName = value;
    }

    /**
     * Gets the value of the uri property.
     * 
     * @return
     *     possible object is
     *     {@link XsdAnyURIType }
     *     
     */
    public XsdAnyURIType getUri() {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     * 
     * @param value
     *     allowed object is
     *     {@link XsdAnyURIType }
     *     
     */
    public void setUri(XsdAnyURIType value) {
        this.uri = value;
    }

    /**
     * Gets the value of the validator property.
     * 
     * @return
     *     possible object is
     *     {@link ValidatorType }
     *     
     */
    public ValidatorType getValidator() {
        return validator;
    }

    /**
     * Sets the value of the validator property.
     * 
     * @param value
     *     allowed object is
     *     {@link ValidatorType }
     *     
     */
    public void setValidator(ValidatorType value) {
        this.validator = value;
    }

    /**
     * Gets the value of the listener property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the listener property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getListener().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ListenerType }
     * 
     * 
     */
    public List<ListenerType> getListener() {
        if (listener == null) {
            listener = new ArrayList<ListenerType>();
        }
        return this.listener;
    }

    /**
     * Gets the value of the tag property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tag property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTag().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TagType }
     * 
     * 
     */
    public List<TagType> getTag() {
        if (tag == null) {
            tag = new ArrayList<TagType>();
        }
        return this.tag;
    }

    /**
     * Gets the value of the tagFile property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tagFile property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTagFile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TagFileType }
     * 
     * 
     */
    public List<TagFileType> getTagFile() {
        if (tagFile == null) {
            tagFile = new ArrayList<TagFileType>();
        }
        return this.tagFile;
    }

    /**
     * Gets the value of the function property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the function property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFunction().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FunctionType }
     * 
     * 
     */
    public List<FunctionType> getFunction() {
        if (function == null) {
            function = new ArrayList<FunctionType>();
        }
        return this.function;
    }

    /**
     * Gets the value of the taglibExtension property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the taglibExtension property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTaglibExtension().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TldExtensionType }
     * 
     * 
     */
    public List<TldExtensionType> getTaglibExtension() {
        if (taglibExtension == null) {
            taglibExtension = new ArrayList<TldExtensionType>();
        }
        return this.taglibExtension;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getVersion() {
        if (version == null) {
            return "2.1";
        } else {
            return version;
        }
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setVersion(java.lang.String value) {
        this.version = value;
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
