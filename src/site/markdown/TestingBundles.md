# Testing Bundles / Plugins with Tycho

There are different ways to test bundles / plug-ins with Tycho:

## maven-surefire-plugin

Using [maven-surefire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) is the preferred way whenever you want to write a plain unit-test.
This is a unit test that either:

* doesn't need an OSGi runtime
* uses some kind of mocking technique for OSGi (e.g. [Apache Sling OSGi Mocks](https://sling.apache.org/documentation/development/osgi-mock.html))
* or starts an embedded OSGi Framework (e.g. [osgi-test-framework](https://github.com/laeubisoft/osgi-test-framework)).

This requires:
- setting up your project using a test-source folder (see below), alternatively using the [standard maven layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html)
- a configured execution of the `maven-surefire-plugin:test` goal
- packaging `eclipse-plugin` is used

A sample snippet looks like this:

```xml
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

```xml
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
this is like `plugin-test` but uses the BND testing framework. There is currently no JDT/PDE equivalent but this integrates nicely with the [OSGi Testing Support](https://github.com/osgi/osgi-test) and allows to execute pre-built test-bundles.

A sample snippet looks like this:

```xml
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

## tycho-test-plugin

The [tycho-test-plugin](https://tycho.eclipseprojects.io/doc/master/tycho-test-plugin/plugin-info.html) is a new plugin introduced in Tycho 6 to provide unified testing of OSGi bundles.
Unlike previous approaches, it is no longer bound to surefire and offers better integration with modern testing frameworks.

### junit-platform mojo

The `tycho-test:junit-platform` mojo integrates the [JUnit Platform Console Launcher](https://docs.junit.org/current/user-guide/#running-tests-console-launcher) into any OSGi Framework.
This approach has several advantages:

1. Tycho is completely independent from the used JUnit framework version (since it calls it via a command-line interface)
2. Better and more natural integration of selecting test engines in the pom.xml or with the target platform
3. You can use any of the JUnit provided test engines or new features that might be added

This requires:
- packaging `eclipse-plugin` is used
- a configured execution of the `tycho-test:junit-platform` goal
- JUnit Platform dependencies (console launcher and test engines) as test-scoped dependencies

A sample snippet looks like this:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/maven-v4_0_0.xsd">
   ...
   <build>
      ...
      <plugin>
         <groupId>org.eclipse.tycho</groupId>
         <artifactId>tycho-test-plugin</artifactId>
         <version>${tycho-version}</version>
         <executions>
            <execution>
               <id>execute-tests</id>
               <goals>
                  <goal>junit-platform</goal>
               </goals>
            </execution>
         </executions>
      </plugin>
   </build>
   
   <dependencies>
      <!-- The API is used at compile time of the bundle -->
      <dependency>
         <groupId>org.junit.jupiter</groupId>
         <artifactId>junit-jupiter-api</artifactId>
         <version>${junit-version}</version>
         <scope>compile</scope>
      </dependency>
      
      <!-- The console and the engine are only required at test execution time -->
      <dependency>
         <groupId>org.junit.platform</groupId>
         <artifactId>junit-platform-console</artifactId>
         <version>${junit-version}</version>
         <scope>test</scope>
      </dependency>
      <dependency>
         <groupId>org.junit.jupiter</groupId>
         <artifactId>junit-jupiter-engine</artifactId>
         <version>${junit-version}</version>
         <scope>test</scope>
      </dependency>
   </dependencies>
</project>
```

To execute the tests, one has to invoke maven with `mvn verify`. The following demo project is provided as an example:

- https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/junit-platform

## combining different approaches

## setup test source folders in eclipse

When working with Eclipse and PDE (Plugin Development Environment), you can mark source folders as containing test sources. This is important for Tycho to correctly identify and compile test classes separately from production code.

### Marking a Source Folder as Test Source

To configure a source folder to contain test sources in Eclipse:

1. **Right-click on your Eclipse plugin project** in the Package Explorer or Project Explorer
2. **Select "Properties"** from the context menu
3. **Navigate to "Java Build Path"** in the left panel
4. **Select the "Source" tab**
5. **Locate the source folder** you want to mark as a test source folder (e.g., `src_test`)
6. **Expand the source folder entry** by clicking on the arrow/triangle next to it to reveal its attributes
7. **Look for "Contains test sources: No"** in the expanded view
8. **Double-click on "Contains test sources: No"** or select it and click "Edit"
9. **In the dialog that appears, change the value to "Yes"**
10. **Click "OK"** to close the edit dialog
11. **Click "Apply and Close"** to save the changes

> **Tip**: After marking a source folder as a test source, you'll see "Contains test sources: Yes" in the build path configuration.

### What This Does

When you mark a source folder as containing test sources, Eclipse modifies the `.classpath` file in your project to include a test attribute. For example:

```xml
<classpathentry kind="src" output="bin_test" path="src_test">
    <attributes>
        <attribute name="test" value="true"/>
    </attributes>
</classpathentry>
```

### How Tycho Uses This Information

Tycho reads the `.classpath` file to determine which source folders contain test code:

- **Source folders without the test attribute** (or with `test="false"`) are treated as production code and compiled during the `compile` phase
- **Source folders with `test="true"`** are treated as test code and compiled during the `test-compile` phase with test dependencies available

This allows you to:
- Keep test and production code in the same project
- Use different output directories for test and production classes
- Have test-specific dependencies that don't leak into your production bundle

### Recommended Directory Structure

For projects that include both production and test code, a common structure is:

```
your-plugin-project/
├── src/              (production code)
├── src_test/         (test code, marked with test="true")
├── META-INF/
│   └── MANIFEST.MF
├── build.properties
└── pom.xml
```

### Important Notes

- The test attribute is supported in Eclipse since version 4.8 (2018-09)
- When using pomless builds, Tycho automatically detects test source folders marked in the `.classpath` file
- Test source folders should be included in the `build.properties` file if you want them to be part of the build
- Make sure your `pom.xml` includes the necessary test plugin configurations (either `maven-surefire-plugin` or `tycho-surefire-plugin` with appropriate executions)

### Alternative: Manual .classpath Editing

If you prefer, you can also directly edit the `.classpath` file in your project root. Add or modify the `classpathentry` element for your test source folder to include the test attribute:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
    <!-- Production source folder -->
    <classpathentry kind="src" output="bin" path="src"/>
    
    <!-- Test source folder with test attribute -->
    <classpathentry kind="src" output="bin_test" path="src_test">
        <attributes>
            <attribute name="test" value="true"/>
        </attributes>
    </classpathentry>
    
    <!-- Other classpath entries -->
    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
    <classpathentry kind="con" path="org.eclipse.pde.core.requiredPlugins"/>
    <classpathentry kind="output" path="bin"/>
</classpath>
```

After editing, refresh your project in Eclipse (F5) for the changes to take effect.

### Example Projects

See these demo projects for working examples:
- [Surefire with source folder](https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/surefire/with-source-folder) - Shows `src_test` marked as test source
- [Tycho standalone test](https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/tycho/standalone/test) - Shows a project with only test sources
- [Tycho OSGi test](https://github.com/eclipse-tycho/tycho/tree/master/demo/testing/tycho/osgitest) - Shows mixed production and test sources
