# Creating SBOMs for Eclipse Artifacts

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

See this demo for a complete, working setup (archive a product for several platforms, then generate an SBOM for each archive):

- https://github.com/eclipse-tycho/tycho/tree/main/demo/sbom-product

### Choosing the input

The generator needs one of two inputs:

- `installation`: a single product, given as an installation folder or as an archive (`*.zip`, `*.tar`, `*.tar.gz`)
- `installations`: a folder containing product **archives** (`*.zip`, `*.tar`, `*.tar.gz`), one SBOM is generated for each archive

An unpacked product folder inside `installations` is ignored, only archives are picked up.
If none of the two is set, the build fails.

A single execution with `installations` covers everything a project produces: one SBOM per archive, so each product and each platform variant (e.g. `linux` and `win32`) is analyzed separately.
The files are named `<archive name>-sbom.xml` and `-sbom.json`.

### using inside the maven build

The mojo is bound to `verify` by default, after the director goals (bound to `package`) created the archives, and requires Java 25.

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-director-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>archive-products</id>
            <goals>
                <goal>archive-products</goal>
            </goals>
        </execution>
    </executions>
</plugin>
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
                <installations>${project.build.directory}/products</installations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

To analyze a single already materialized product instead, use `installation` and point it to
`${project.build.directory}/products/<product uid>/<os>/<ws>/<arch>` (requires `materialize-products`).

### calling from CLI

The mojo does not need a project, so it can be called on any existing product or installation:

```
mvn org.eclipse.tycho:tycho-sbom-plugin:generator -Dinstallation=<folder or archive> -Dprint.xml
mvn org.eclipse.tycho:tycho-sbom-plugin:generator -Dinstallations=<folder with archives>
```

Without a project the output files are written to the `target` folder of the current directory (named `standalone-pom.xml`/`.json` for `installation`).
Use `-Dxml-outputs=<folder>`, `-Djson-outputs=<folder>` and `-Dindex=<file>` to change this, and `-Dprint.xml` to print the SBOM to the console.

### What is in the SBOM

For every analyzed installation the SBOM lists the bundles, features and other artifacts of the installation as CycloneDX components, together with their dependencies.
Each component has hashes, a PURL, and where available publisher, description, license and links to the website and source repository (from the manifest and the p2 metadata).
The metadata is richer when enabling:

- `centralsearch`: artifacts that are also on Maven Central get a `pkg:maven` PURL (and with this usually license information)
- `p2sources`: artifacts found in the given p2 repositories get PURLs pointing to them instead of local `file:` locations; artifacts built locally keep the `file:` URL
- `advisory`: known vulnerabilities from OSV are added as external references

Without these, most PURLs are `pkg:p2` with a local `file:` repository URL, which is of little use outside of the build machine.

### Output

All outputs are written to `${project.build.directory}` by default:

- `installations`: `<archive name>-sbom.xml` and `<archive name>-sbom.json` per archive
- `installation`: `<artifactId>.xml` and `<artifactId>.json`
- `index.html`: an overview of the generated SBOMs with links to the [online renderer](https://download.eclipse.org/cbi/sbom/) (see [Viewing SBOMs](#viewing-sboms))

### Viewing SBOMs

The generated `index.html` links to the renderer hosted at https://download.eclipse.org/cbi/sbom/.
Due to browser cross-origin restrictions this only works if the SBOMs are hosted on the same origin as the renderer, so for local files it fails.
There are three alternatives:

- paste the content of an `.xml` file into the `SBOM Text` field of the online renderer
- host the renderer yourself: it is a single static page (the [`www` folder](https://github.com/eclipse-cbi/p2repo-sbom/tree/main/www) of p2repo-sbom).
  Copy it for example to `target/sbom`, serve `target` with any web server (e.g. `python3 -m http.server 8123` in `target`) and generate with `-Drenderer=http://localhost:8123/sbom`.
  The links in `index.html` are then relative to the index, so the folder with the index and the SBOMs can be moved to any web server together with the renderer
- use the [p2repo-sbom IDE integration](https://github.com/eclipse-cbi/p2repo-sbom/blob/main/docs/ide-guide.md) (update site `https://download.eclipse.org/cbi/updates/p2-sbom/tools/nightly/latest`), it provides an integrated renderer that can open the SBOMs and the `index.html`

### Configuration Parameters

The `tycho-sbom:generator` mojo supports the following configuration parameters:

- **`installations`** (File, property: `installations`): A folder containing multiple packaged products (archives) to be analyzed, one SBOM is created for each
- **`installation`** (File, property: `installation`): A single installation directory, product archive or update-site to analyze
- **`cache`** (File): Specify a cache location for downloaded metadata. If not specified, Tycho uses its global cache location in the Maven local repository
- **`centralsearch`** (boolean, property: `central-search`): If enabled, artifacts are mapped to Maven Central using file hashcodes. When a unique match is found, it's assumed to be the real source even if P2 has not recorded any GAVs
- **`advisory`** (boolean, property: `advisory`): If enabled, queries the Open Source Vulnerabilities (OSV) distributed vulnerability database for known vulnerabilities in Open Source components and adds them as external references to the components
- **`p2sources`** (List of String): A list of URIs that should be used to match against P2 units. These are typically the repositories used during product build
- **`verbose`** (boolean, property: `sbom.verbose`): Enable verbose logging output from the generator
- **`xmlOutputs`** (File, property: `xml-outputs`, default: `${project.build.directory}`): Directory where XML SBOM files will be written
- **`jsonOutputs`** (File, property: `json-outputs`, default: `${project.build.directory}`): Directory where JSON SBOM files will be written
- **`index`** (File, property: `index`, default: `${project.build.directory}/index.html`): Path where the HTML index file will be written
- **`print.xml`** (boolean, property: `print.xml`): Print the generated XML SBOM to the console
- **`process-bundle-classpath`** (boolean): Inspect the bundle classpath of each bundle and create sub-components for nested jars
- **`content-redirections`** (List of String, property: `content-redirections`): URI redirections in the form `source->target` (both ending in `/`) used when looking up content, e.g. to use a Maven Central mirror
- **`maven-lookup-exclusions`** (List of String, property: `maven-lookup-exclusions`): Regular expressions matched against Maven coordinates (`groupId:artifactId:version[:classifier]`), matching artifacts are excluded from the Maven lookup
- **`format-version`** (String, property: `format-version`): The CycloneDX specification version used to write the SBOM (e.g. `1.6`), defaults to the latest supported
- **`renderer`** (String, property: `renderer`): Base URL of the SBOM renderer used for the links in the index, must be on the same origin as the SBOMs
- **`preview`** (String, property: `preview`): A `source->target` redirection of the index location to where it is hosted, if the result is not a `file:` URI it is opened in the browser
- **`generatorRepository`** (Repository): The repository where the generator application should be sourced from
