<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<modelVersion>4.0.0</modelVersion>
	<groupId>${project.getGroupId()}</groupId>
	<artifactId>${project.getArtifactId()}</artifactId>
	<version>${project.getVersion()}</version>
	<packaging>${project.getPackaging()}</packaging>
<#if project.getDependencies()?has_content>

	<dependencies>
<#list project.getDependencies() as dependency>
		<dependency>
			<groupId>${dependency.getGroupId()}</groupId>
			<artifactId>${dependency.getArtifactId()}</artifactId>
			<version>${dependency.getVersion()}</version>
		</dependency>
</#list>
	</dependencies>
</#if>
</project>
