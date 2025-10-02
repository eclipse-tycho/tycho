# Eclipse Tycho 6: Release notes

This page describes the noteworthy improvements provided by each release of Eclipse Tycho.
If you are reading this in the browser, then you can quickly jump to specific versions by using the rightmost button above the headline:
![grafik](https://github.com/eclipse-tycho/tycho/assets/406876/7025e8cb-0cdb-4211-8239-fc01867923af)

## 6.0.0 (under development)

### new `tycho-p2-extras:p2-manager` mojo for managing P2 update sites

The new `tycho-p2-extras:p2-manager` goal provides a convenient way to maintain, update, and manage the integrity of public update sites. This mojo wraps the [P2 Manager application from JustJ Tools](https://eclipse.dev/justj/?page=tools) and makes it much easier to use compared to the previous approach using the eclipse-run goal.

The P2 Manager helps with:
- Promoting builds to update sites (nightly, milestone, or release)
- Generating composite repositories
- Managing repository history and retention policies
- Creating browsable HTML pages for your update sites
- Maintaining repository integrity

```xml
<plugin>
    <groupId>org.eclipse.tycho.extras</groupId>
    <artifactId>tycho-p2-extras-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>promote-build</id>
            <goals>
                <goal>p2-manager</goal>
            </goals>
            <configuration>
                <root>${project.build.directory}/updatesite</root>
                <promote>file:${project.build.directory}/repository</promote>
                <timestamp>${maven.build.timestamp}</timestamp>
                <type>nightly</type>
            </configuration>
        </execution>
    </executions>
</plugin>
```

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


### Migration Guide 5.x > 6.x


## 5.x

For release notes of the Tycho 5 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-5.0.x/RELEASE_NOTES.md)

## 4.x

For release notes of the Tycho 4 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-4.0.x/RELEASE_NOTES.md)

## 3.x

For release notes of the Tycho 3 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-3.0.x/RELEASE_NOTES.md)

## 2.x

For release notes of the Tycho 2 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-2.7.x/RELEASE_NOTES.md)