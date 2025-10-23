# Eclipse Tycho 6: Release notes

This page describes the noteworthy improvements provided by each release of Eclipse Tycho.
If you are reading this in the browser, then you can quickly jump to specific versions by using the rightmost button above the headline:
![grafik](https://github.com/eclipse-tycho/tycho/assets/406876/7025e8cb-0cdb-4211-8239-fc01867923af)

## 6.0.0 (under development)

### new `tycho-test-plugin` for unified testing of OSGi bundles

Historically Tycho has provided something similar to [maven-surefire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) and also actually using it internally.
This was to give a kind of "like maven" and as surefire is acting as an abstraction over different test-engines and offering a unified configuration, but this has had its shortcomings:

- surefire was never designed to run inside OSGi so we have to use several kinds of workarounds to mitigate for that fact
- as we need special setup and consideration, we never really extended the surefire mojo and only emulated to be similar to maven-surefire-plugin settings leading to slight diverging over time
- due to the design we often are bound to the specific test framework by a so called "provider" that helps in setting things up so it plays well with OSGi requirements and surefire demands
- In the meanwhile there are other techniques like junit-platform that offer similar features like surefire (e.g. the abstraction of test engines) or better integrate into OSGi (e.g. bnd-testing framework) and new features have to be implemented that are often missing.

This now introduces a new `tycho-test-plugin` to make clear it is no longer bound to surefire and open it up for a more unified testing of OSGi bundles, e.g. we might offer different implementations.

#### the new `tycho-test:junit-platform` mojo

With that new approach we now support a new `tycho-test:junit-platform` that integrates the [tests-console-launcher](https://docs.junit.org/current/user-guide/#running-tests-console-launcher) into any OSGi Framework
what has several advantages over the previous approaches:

1. As we are calling it via a commandline interface, this now makes Tycho completely independent from the used junit-framework version
2. A better and more natural integration of selecting test-engines in the pom.xml or with the target platform
3. You can use any of the junit provided test engines or new features that might be added

You can find a demo project [here}(https://github.com/eclipse-tycho/tycho/tree/main/demo/testing/junit-platform).

### new `tycho-wrap:verify` to test a maven-project with a generated manifest can resolve

A common way to ensure compatibility and integration of java libraries is to enable the generation of an OSGi manifest automatically.
As one can not expect such project to be OSGi experts there is often a problem that these do not feel comfortable with adding such without any mean to validate the outcome.
Also it is often not obvious when using a new dependency if this would hinder integration with OSGi or to ensure the actual result is usable without complex and hard to maintain full
blown integration-test scenarios that project hardly can handle over a long time.

This now introduces a new `tycho-wrap:verify` mojo that tries to fill the gap here between full integration testing and a basic validation with the intention to give clear hint how to handle issues.

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-wrap-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <goals>
                <goal>verify</goal>
            </goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
```

A very important part here is that it is possible to ignore problems (either temporary or permanent) so it never completely blocks further development:

```xml
<execution>
    <goals>
        <goal>verify</goal>
    </goals>
    <phase>verify</phase>
    <configuration>
    <ignored>
        <!-- This is currently not an OSGi bundle, we would need help from other to resolve the problem, so please feel free to suggest ways to fix this -->
        <ignore>Import-Package: x.y.z</ignore>
        <!-- We are working on this, will be fixed in next release cycle -->
        <ignore>Import-Package: work.in.progress</ignore>
    </ignored>
    </configuration>
</execution>
```

This has likely to improve over time when we find new issues, or get feedback from users!

### new `tycho-sbom:generator mojo` to create SBOM from existing products

While creating SBOMs for individual reactor projects is [already possible](https://github.com/eclipse-tycho/tycho/blob/tycho-5.0.x/RELEASE_NOTES.md#support-for-cyclonedx-maven-plugin)
It is often more useful to generate one for an actual deployed product, as these can be extended afterwards. Also there are some short comings when it comes to identify licenses.

There is currently an [eclipse tool](https://github.com/eclipse-cbi/p2repo-sbom/blob/main/docs/index.md) in development that allows feed with a product and then gathers a lot of
information from different sources (e.g. maven central, clearly defined, ...) and generates a SBOM from that.

Tycho now supports calling this tool from CLI (e.g. `mvn org.eclipse.tycho:tycho-sbom-plugin:6.0.0-SNAPSHOT:generator -Dinstallations=<some folder>`) or as part of a maven build:


```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-sbom-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
      <execution>
        <id>generate-sbom</id>
        <goals>
          <goal>generator</goal> 
        </goals>
        <configuration>
            <installations>${project.build.directory}/target/myproducts</installations>
        </configuration>
      </execution>
    </executions>
</plugin>
```

### new `tycho-dependency-tools:usage` mojo to analyze target platform dependency usage

Managing target platform dependencies can be challenging, especially when it comes to identifying which dependencies are actually needed versus which can be removed.
The new `tycho-dependency-tools:usage` mojo helps solve this problem by analyzing the actual usage of dependencies from target platform definitions across all projects in the reactor.

This mojo compares the content of target definitions against what is actually used in your projects, making it easy to:
- Identify unused dependencies that can be safely removed
- See which dependencies are used directly versus indirectly (through transitive dependencies)
- Understand the relationship between target platform units and the projects that use them

You can run it from the command line:

```bash
mvn org.eclipse.tycho.extras:tycho-dependency-tools-plugin:6.0.0-SNAPSHOT:usage
```

The mojo supports two report layouts:
- **tree** (default): Organizes units by target file and location with a hierarchical view
- **simple**: One-line-per-unit format

You can switch layouts using the `usage.layout` property:

```bash
mvn org.eclipse.tycho.extras:tycho-dependency-tools-plugin:6.0.0-SNAPSHOT:usage -Dusage.layout=simple
```

For more detailed output showing which projects use each dependency, add the `verbose` flag:

```bash
mvn org.eclipse.tycho.extras:tycho-dependency-tools-plugin:6.0.0-SNAPSHOT:usage -Dverbose=true
```

### Migration Guide 5.x > 6.x


## 5.x

For release notes of the Tycho 5 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-5.0.x/RELEASE_NOTES.md)

## 4.x

For release notes of the Tycho 4 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-4.0.x/RELEASE_NOTES.md)

## 3.x

For release notes of the Tycho 3 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-3.0.x/RELEASE_NOTES.md)

## 2.x

For release notes of the Tycho 2 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-2.7.x/RELEASE_NOTES.md)