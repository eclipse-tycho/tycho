# Eclipse Tycho: Release notes

This page describes the noteworthy improvements provided by each release of Eclipse Tycho.

### Next release...

## 2.4.0

### [Execute unit-tests with eclipse-plugin packaging](https://bugs.eclipse.org/bugs/show_bug.cgi?id=572420) 
Before unit-tests are only executed for eclipse-test-plugin packaging types. Beside that it was only possible to execute them as part of **tycho-surefire** (what executes them inside an OSGi runtime) in the interation-test-phase (making them actually some kind of integration test).

From now on this restriction is no longer true and one is able to execute unit-test with **maven-surefire** as well as integration-tests with **tycho-failfast** plugin. This works the following way:

 - create a source-folder in your eclipse-plugin packaged project and mark them as a contains test-sources in the classpath settings:![grafik](https://user-images.githubusercontent.com/1331477/116801917-b20cb080-ab0e-11eb-8c05-1796196ccb25.png)
 - Create a unit-test inside that folder, either name it with any of the [default-pattern](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#includes) maven-surefire plugin of or configure them explicitly.
 - Include maven-surefire plugin configuration in your pom to select the approriate test-providers
```
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-surefire-plugin</artifactId>
	<version>3.0.0-M5</version>
	<dependencies>
		<dependency>
			<groupId>org.apache.maven.surefire</groupId>
			<artifactId>surefire-junit47</artifactId>
			<version>3.0.0-M5</version>
		</dependency>
	</dependencies>
</plugin>
```
 - run your it with `mvn test`

Tycho also includes a new tycho-failsafe mojo, that is similar to the maven one:
 - it executes at the integration-test phase but do not fail the build if a test fails, instead a summary file is written
 - the outcome of the tests are checked in the verify phase (and fail the build there if neccesary)
 - this allows to hook some setup/teardown mojos (e.g. start webservers, ...) in the pre-integration-test phase and to safly tear them down in the post-integration test phase (thus the name 'failsafe' see [tycho-failsafe-faq](https://maven.apache.org/surefire/maven-failsafe-plugin/faq.html) for some more details

Given you have the above setup you create an integration-test (executed in an OSGi runtime like traditional tycho-surefire mojo) as following:

- create a new test that mathes the pattern `*IT.java` (or configure a different pattern that do not intersects with the surefire test pattern)
- run your it with `mvn verify`

:warning: If you where previously using `-Dtest=....` on the root level of your build tree it might now be neccesary to also include `-Dsurefire.failIfNoSpecifiedTests=false` as maven-surefire might otherwhise complain about 

> No tests were executed! (Set -DfailIfNoTests=false to ignore this error.)

for your eclipse-plugin packaged project if they do not match anything (the error message is a bit missleading, thsi is tracked in [SUREFIRE-1910](https://issues.apache.org/jira/browse/SUREFIRE-1910)).


### [Enhanced support for debug output in surefire-tests](https://github.com/eclipse/tycho/issues/52) 
tycho-surefire now support to set .options files for debugging through the new debugOptions parameter, example: 

```
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-surefire-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <showEclipseLog>true</showEclipseLog>
    <debugOptions>${project.basedir}/../../debug.options</debugOptions>
</configuration>
</plugin>
  ```
