# Creating SBOMs for Tycho artifacts

Tycho has two ways of creating an SBOM for your artifacts one local using an extension for the [cyclone-dx plugin](https://github.com/CycloneDX/cyclonedx-maven-plugin)
and one more global approach using a prebuild products.

## Using the cyclone-dx extension

```xml
<plugin>
    <groupId>org.cyclonedx</groupId>
    <artifactId>cyclonedx-maven-plugin</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.eclipse.tycho</groupId>
            <artifactId>tycho-sbom</artifactId>
            <version>${tycho-version}</version>
        </dependency>
    </dependencies>
</plugin>
```

see also this demo:

- https://github.com/eclipse-tycho/tycho/tree/main/tycho-its/projects/sbom

## Using a prebuild product

### calling from CLI

`mvn org.eclipse.tycho:tycho-sbom-plugin:6.0.0-SNAPSHOT:generator -Dinstallations=<some folder>`

### using inside the maven build

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