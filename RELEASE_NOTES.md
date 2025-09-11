# Eclipse Tycho 6: Release notes

This page describes the noteworthy improvements provided by each release of Eclipse Tycho.
If you are reading this in the browser, then you can quickly jump to specific versions by using the rightmost button above the headline:
![grafik](https://github.com/eclipse-tycho/tycho/assets/406876/7025e8cb-0cdb-4211-8239-fc01867923af)

## 6.0.0 (under development)

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