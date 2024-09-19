# Testing Bundles / Plugins with Tycho

There are different ways to test bundles / plug-ins with Tycho:

## maven-surefire-plugin

Using [maven-surefire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) is the preferred way whenever you want to write a plain unit-test.
This is a unit test that either:

* don't need a OSGi runtime
* use some kind of mocking technique for OSGi (e.g. [Apache Sling OSGi Mocks](https://sling.apache.org/documentation/development/osgi-mock.html))
* or starts an embedded OSGi Framework (e.g. [osgi-test-framework](https://github.com/laeubisoft/osgi-test-framework)).

This requires:
- setting up your project using a test-source folder (see below), alternatively using the [standard maven layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html)
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

To execute the tests, one has to invoke maven with `mvn test`.
The following demo projects are provided as an example:

- Project with a configured source folder: https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/surefire/with-source-folder
- Project using maven standard layout: https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/surefire/with-maven-layout


## tycho-surefire-plugin

The [tycho-surefire-plugin](https://tycho.eclipseprojects.io/doc/master/tycho-surefire-plugin/plugin-info.html) is the preferred whenever you want to write tests
that require an OSGi Framework running and is executed in the integration-test phase of your build, this is similar to what PDE offers as Plugin Tests.

There are two ways to use this:

1. You use the `eclipse-test-plugin` packaging, and with those your plugin must only contain test-classes and they will be executed automatically as part
of the integration-test phase of your build. This approach is not recommended for new designs.
2. You use `eclipse-plugin` packaging and configure an additional execution of the `tycho-surefire-plugin:plugin-test` goal with either a test-source folder
 (see below), alternatively using the [standard maven layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html).
 
A sample snippet looks like this:

```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/maven-v4_0_0.xsd">
	...
	<build>
		...
		<plugin>
			<groupId>org.eclipse.tycho</groupId>
			<artifactId>tycho-surefire-plugin</artifactId>
			<version>${tycho-version}</version>
			<executions>
				<execution>
					<id>execute-integration-tests</id>
					<goals>
						<goal>plugin-test</goal>
						<goal>verify</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
 	</build>
 </project>
```

To execute the tests, one must invoke maven with `mvn verify`, the following demo projects are provided as an example:

- Project with a configured source folder as a standalone project similar to the discouraged `eclipse-test-plugin` packaging:
https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/tycho/standalone
- Project using maven standard layout having the tests in the same module: 
https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/tycho/samemodule

## bnd-testing

The [tycho-surefire-plugin](https://tycho.eclipseprojects.io/doc/master/tycho-surefire-plugin/plugin-info.html) has also support for [bnd-testing](https://bnd.bndtools.org/chapters/310-testing.html),
this is like `plugin-test` but uses the BND testing framework. There is currently no JDT/PDE equivalent but this integrates nicely with the [OSGi Testing Support](https://github.com/osgi/osgi-test) and allows to execute prebuild test-bundles.

A sample snippet looks like this:

```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/maven-v4_0_0.xsd">
	...
	<build>
		...
		<plugin>
			<groupId>org.eclipse.tycho</groupId>
			<artifactId>tycho-surefire-plugin</artifactId>
			<version>${tycho-version}</version>
			<executions>
				<execution>
					<id>execute-integration-tests</id>
					<goals>
						<goal>bnd-test</goal>
						<goal>verify</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
 	</build>
 </project>
```

To execute the tests, one has to invoke maven with `mvn verify`, the following demo projects are provided as an example:

- Project with maven standard layout having the tests in the same module and using [OSGi JUnit5 annotations](https://github.com/osgi/osgi-test/blob/main/org.osgi.test.junit5/README.md)
to automatically inject services: 

- https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/bnd/osgi-test



## combining different approaches 

## setup test source folders in eclipse
