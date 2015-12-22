<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  
  <xsl:template match="*[namespace-uri() = 'http://java.sun.com/xml/ns/javaee']">
    <xsl:element name="{local-name()}" namespace="http://xmlns.jcp.org/xml/ns/javaee">
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
    </xsl:element>
  </xsl:template>

  <xsl:template match="node()">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates select="node()" />
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>