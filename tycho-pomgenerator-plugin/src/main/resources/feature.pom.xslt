<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/feature">
			<dependencies>
				<xsl:apply-templates select="/feature/plugin" />
			</dependencies>
	</xsl:template>

	<xsl:template match="plugin">
		<dependency>
			<groupId>$GROUPID$</groupId>
			<artifactId><xsl:value-of select="@id"/></artifactId>
			<version><xsl:value-of select="@version"/></version>
		</dependency>
	</xsl:template>

</xsl:stylesheet>