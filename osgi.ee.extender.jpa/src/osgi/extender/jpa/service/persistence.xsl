<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:p="http://xmlns.jcp.org/xml/ns/persistence" version="1.0">
  <xsl:output method="xml" indent="yes"/>
  <xsl:template match="/">
    <list>
      <xsl:apply-templates select="p:persistence"/>
    </list>
  </xsl:template>
  
  <xsl:template match="p:persistence/p:persistence-unit">
    <object class="osgi.extender.jpa.service.PersistenceUnitDefinition">
      <name><xsl:value-of select="@name"/></name>
      <version><xsl:value-of select="parent::*/@version"/></version>
      <transactionType><xsl:value-of select="@transaction-type"/></transactionType>
      <description><xsl:value-of select="p:description"/></description>
      <provider><xsl:value-of select="p:provider"/></provider>
      <jtaDs><xsl:value-of select="p:jta-data-source"/></jtaDs>
      <nonJtaDs><xsl:value-of select="p:non-jta-data-source"/></nonJtaDs>
      <mappingFiles>
        <xsl:apply-templates select="p:mapping-file" mode="string"/>
      </mappingFiles>
      <jarFiles>
        <xsl:apply-templates select="p:jar-file" mode="string"/>
      </jarFiles>
      <classes>
        <xsl:apply-templates select="p:class" mode="string"/>
      </classes>
      <excludeUnlisted><xsl:value-of select="p:exclude-unlisted-classes"/></excludeUnlisted>
      <cachingType><xsl:value-of select="p:shared-cache-mode"/></cachingType>
      <validationMode><xsl:value-of select="p:validation-mode"/></validationMode>
      <properties>
        <xsl:for-each select="p:properties/p:property">
          <object class="osgi.extender.jpa.service.Property">
            <key><xsl:value-of select="@name"/></key>
            <value><xsl:value-of select="@value"/></value>
          </object>
        </xsl:for-each>
      </properties>
    </object>
  </xsl:template>
  
  <xsl:template match="*" mode="string">
    <string><xsl:value-of select="."/></string>
  </xsl:template>
</xsl:stylesheet>