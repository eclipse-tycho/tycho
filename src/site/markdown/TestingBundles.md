## Testing Bundles / Plugins with Tycho

There are different ways to test Bundles / Plugins with Tycho:

### maven-surefire-plugin

Using [maven-surefire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) is the preferred way whenever you want to write a plain unit-test,
that is one that either don't need a running OSGi, use some kind of mocking technique (e.g. [Apache Sling OSGi Mocks](https://sling.apache.org/documentation/development/osgi-mock.html))
or starts an embedded OSGi Framework (e.g. [osgi-test-framework](https://github.com/laeubisoft/osgi-test-framework)).

This requires:
- setting up your project using a test-source folder (see below), alternatively using the standard maven layout
- an configured execution of the `maven-surefire-plugin:test` goal
- packaging `eclipse-plugin` is used

A sample snippet looks like this:
```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/maven-v4_0_0.xsd">
	...
	<build>
		...
		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-surefire-plugin</artifactId>
			<version>${surefire-plugin-version}</version>
			<executions>
				<execution>
					<id>execute-tests</id>
					<goals>
						<goal>test</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
 	</build>
 </project>
```

There are two demo projects that show a running example:

1. Project with a configured source folder: https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/surefire/with-source-folder
2. Project using maven standard layout: https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/surefire/with-maven-layout


### tycho-surefire-plugin

### combining different approaches 

### setup test source folders in eclipse