# Creating SBOMs for Tycho artifacts

## What is an SBOM?

A Software Bill of Materials (SBOM) is a formal, machine-readable inventory of software components and dependencies, information about those components, and their hierarchical relationships. SBOMs are essential for:

- **Security**: Understanding your software supply chain and identifying vulnerable components
- **Compliance**: Meeting regulatory requirements and license obligations
- **Transparency**: Documenting what's in your software for customers and stakeholders

Tycho supports generating SBOMs in industry-standard formats like CycloneDX, making it easier to maintain visibility into your Eclipse-based applications.

## SBOM Generation Approaches

Tycho has two ways of creating an SBOM for your artifacts one local using an extension for the [cyclone-dx plugin](https://github.com/CycloneDX/cyclonedx-maven-plugin)
and one more global approach using a prebuild products.

## Using the cyclone-dx extension

The [CycloneDX Maven Plugin](https://github.com/CycloneDX/cyclonedx-maven-plugin) creates SBOMs for individual Maven projects during the build lifecycle. Tycho provides an extension (`tycho-sbom`) that enables the CycloneDX plugin to properly understand and include OSGi/Eclipse-specific artifacts in the generated SBOM.

This approach is ideal for:
- Generating SBOMs as part of your regular Maven build
- Creating per-module SBOMs for individual components
- Integration with CI/CD pipelines

To use this extension, add the CycloneDX Maven Plugin with the Tycho SBOM dependency:

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

For more information about the CycloneDX Maven Plugin, including configuration options and best practices, see the [official CycloneDX Maven Plugin documentation](https://github.com/CycloneDX/cyclonedx-maven-plugin).

See also this demo:

- https://github.com/eclipse-tycho/tycho/tree/main/tycho-its/projects/sbom

## Using a prebuild product

The `tycho-sbom:generator` mojo wraps the [Eclipse CBI p2repo-sbom tool](https://github.com/eclipse-cbi/p2repo-sbom) as a Maven plugin, enabling SBOM generation for complete Eclipse products and installations. This tool is particularly powerful as it:

- Analyzes complete product installations rather than individual build artifacts
- Gathers metadata from multiple sources including Maven Central and ClearlyDefined
- Provides enhanced license identification and dependency mapping
- Generates comprehensive SBOMs that can be extended with additional metadata

This approach is ideal for:
- Generating SBOMs for final, assembled products
- Post-build SBOM generation from existing installations
- Creating SBOMs with enriched metadata from external sources

For detailed information about the underlying tool, see the [p2repo-sbom documentation](https://github.com/eclipse-cbi/p2repo-sbom/blob/main/docs/index.md).

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

### Configuration Parameters

The `tycho-sbom:generator` mojo supports the following configuration parameters:

- **`installations`** (File): Specify a folder where multiple packaged products are located to be analyzed
- **`installation`** (File): Specify a single installation directory or update-site to analyze
- **`cache`** (File): Specify a cache location for downloaded metadata. If not specified, Tycho uses its global cache location in the Maven local repository
- **`central-search`** (boolean, property: `central-search`): If enabled, artifacts are mapped to Maven Central using file hashcodes. When a unique match is found, it's assumed to be the real source even if P2 has not recorded any GAVs
- **`advisory`** (boolean, property: `advisory`): If enabled, queries the Open Source Vulnerabilities (OSV) distributed vulnerability database for known vulnerabilities in Open Source components and adds them as external references to the components
- **`p2sources`** (List of String): A list of URIs that should be used to match against P2 units. These are typically the repositories used during product build
- **`verbose`** (boolean, property: `sbom.verbose`): Enable verbose logging output from the generator
- **`xmlOutputs`** (File, property: `xml-outputs`, default: `${project.build.directory}`): Directory where XML SBOM files will be written
- **`jsonOutputs`** (File, property: `json-outputs`, default: `${project.build.directory}`): Directory where JSON SBOM files will be written
- **`index`** (File, property: `index`, default: `${project.build.directory}/index.html`): Path where the HTML index file will be written
- **`generatorRepository`** (Repository): The repository where the generator application should be sourced from