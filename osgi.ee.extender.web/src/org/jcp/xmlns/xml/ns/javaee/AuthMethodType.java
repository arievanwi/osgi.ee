//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.12.13 at 07:41:28 PM CET 
//


package org.jcp.xmlns.xml.ns.javaee;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 
 *         The auth-methodType is used to configure the authentication
 *         mechanism for the web application. As a prerequisite to
 *         gaining access to any web resources which are protected by
 *         an authorization constraint, a user must have authenticated
 *         using the configured mechanism. Legal values are "BASIC",
 *         "DIGEST", "FORM", "CLIENT-CERT", or a vendor-specific
 *         authentication scheme.
 *         
 *         Used in: login-config
 *         
 *       
 * 
 * <p>Java class for auth-methodType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="auth-methodType">
 *   &lt;simpleContent>
 *     &lt;restriction base="&lt;http://xmlns.jcp.org/xml/ns/javaee>string">
 *     &lt;/restriction>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auth-methodType")
public class AuthMethodType
    extends String
{


}
