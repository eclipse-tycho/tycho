# Eclipse Tycho: Release notes

This page describes the noteworthy improvements provided by each release of Eclipse Tycho.
If you are reading this in the browser, then you can quickly jump to specific versions by using the rightmost button above the headline:
![grafik](https://github.com/eclipse-tycho/tycho/assets/406876/7025e8cb-0cdb-4211-8239-fc01867923af)

## 5.0.0 (under development)

## Support for implicit dependencies in target definitions

In target definitions Tycho now supports to use the `<implicitDependencies>`, 
see [Eclipse Help](https://help.eclipse.org/latest/topic/org.eclipse.pde.doc.user/guide/tools/editors/target_editor/environment_page.htm)
for more details.

## Support for version ranges and no version for units in target definitions

In target definitions Tycho now supports to use a range as version of a unit or to skip the version entirely in `InstallableUnit` locations, just like Eclipse-PDE.
Specifying no version is equivalent to `0.0.0` which resolves to the latest version available.
All of the following variants to specify a version are now possible:

```
<target name="my-target">
    <locations>
        <location includeAllPlatforms="false" includeConfigurePhase="true" includeMode="planner" includeSource="true" type="InstallableUnit">
            <repository location="https://download.eclipse.org/releases/2024-09/"/>
            <unit id="org.eclipse.pde.feature.group" version="3.16.0.v20240903-0240"/>
            <unit id="jakarta.annotation-api" version="0.0.0"/>
            <unit id="org.eclipse.sdk"/>
            <unit id="jakarta.inject.jakarta.inject-api" version="[1.0,2)"/>
        </location>
    </locations>
</target>
```

## new `update-manifest` mojo

It is recommended to use as the lower bound the dependency the code was
compiled with to avoid using newer code from dependencies, but managing
that manually can be a daunting task.

There is now a new `tycho-version-bump:update-manifest` mojo that helps in calculate the
lower bound and update the manifest accordingly.

## new `wrap` mojo

With maven, jars (or more general artifacts) can be build in numerous ways, not all include
the maven-jar-plugin (e.g. maven-assembly-plugin) and not all are easily
combined with maven-bundle or bnd-maven plugin.

Tycho now provides a new `tycho-wrap:wrap mojo` that closes this gap by allowing to
specify an arbitrary input and output, some bnd instructions and (optionally) attach the result to the maven project.

This has the advantage that projects are able to publish two "flavors" of their artifact a plain one and an OSGi-fied one that could
help to convince projects to provide such things as it has zero influence to their build and ways how they build artifacts.

In the simplest form it can be used like this:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-wrap-plugin</artifactId>
    <version>5.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>make-bundle</id>
            <goals>
                <goal>wrap</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## support bumping maven target locations

The `tycho-version-bump-plugin:update-target` now also supports bumping maven target locations to the latest version.

### Support for new `includeJRE` flag when building products

PDE recently added a new flag for the product to mark it to [include a JRE](https://github.com/eclipse-pde/eclipse.pde/pull/1075).
This is now supported by Tycho. Activating this flag has the following effects:

- The product gets a new requirement for a JustJ JRE.
- The JustJ update site is automatically added to the `materialize-products` goal if such product is present.

There is [a demo project](https://github.com/eclipse-tycho/tycho/tree/main/demo/justj/automaticInstall) which shows an example for a product using that flag and including a JRE that is suitable to launch the product automatically.

### Support for CycloneDX Maven Plugin

The `tycho-sbom` plugin can be added as a dependency to the [CycloneDX Maven plugin](https://cyclonedx.github.io/cyclonedx-maven-plugin/index.html),
in order to handle the PURL creation of p2 artifacts:

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

### Support for parallel execution of product assembly/archiving

The mojos `materialize-products` and `archive-products` now support a new `<parallel>` parameter
that enables the parallel assembly/packaging of different product variants.

You can enable this for example like this:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-director-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <parallel>true</parallel>
    </configuration>
</plugin>
```


### New `repo-to-runnable` mojo

This is a replacement for the [Repo2Runnable ant task](https://wiki.eclipse.org/Equinox/p2/Ant_Tasks#Repo2Runnable), example:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-repository-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>repo-to-runnable</id>
            <goals>
                <goal>repo-to-runnable</goal>
            </goals>
            <phase>pre-integration-test</phase>
            <configuration>
                <source>...</source>
                <destination>...</destination>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Support for embedded target locations

You can already define target definition files in various ways, e.g. as maven artifact or file reference.
Now it is also possible to define them as an embedded part of the target platform configuration.
All locations are handled as if they are part of a single target file:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>target-platform-configuration</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <target>
            <!-- one or more location elements like in a target file -->
            <location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="true" missingManifest="generate" type="Maven">
                <dependencies>
                    <dependency>
                        ...
                    </dependency>
                </dependencies>
            </location>
            <location includeAllPlatforms="true" includeMode="slicer" type="InstallableUnit">
                <unit id="org.eclipse.license.feature.group" version="2.0.2.v20181016-2210"/>
                ...
                <repository location="https://download.eclipse.org/cbi/updates/license/2.0.2.v20181016-2210"/>
            </location>
            ...
        </target>
    </configuration>
</plugin>
```

This is especially useful if you need some content only for the build but not in the IDE.

### Using javac as the compiler for Tycho

You can now use `javac` as the compiler backend for Tycho by adding the following configuration:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-compiler-plugin</artifactId>
    <version>${tycho.version}</version>
    <configuration>
        <compilerId>javac</compilerId>
    </configuration>
</plugin>
```


### New `mirror-target-platform` mojo

There is a new `mirror-target-platform` that allows to mirror the current target platform of a project into a P2 update site.
This can be enabled for a project like this:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>target-platform-configuration</artifactId>
    <executions>
        <execution>
            <id>inject</id>
            <goals>
                <goal>mirror-target-platform</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The most usual use case for this is to transform an existing target file into a standalone repository.

### new `director` mojo

This mojo can be used in two ways:

1. As a command line invocation passing arguments as properties using `mvn org.eclipse.tycho:tycho-p2-director-plugin:director -Ddestination=[target] ... -D...`
2. as an execution inside a POM

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-director-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <goals>
                <goal>director</goal>
            </goals>
            <phase>package</phase>
            <configuration>
                <destination>...</destination>
                ... other arguments ...
            </configuration>
        </execution>
    </executions>
 </plugin>
```


### New `tycho-eclipse-plugin`

Tycho now contains a new `tycho-eclipse-plugin` that is dedicated to executing "tasks like eclipse".
This currently includes
- the former tycho-extras `tycho-eclipserun-plugin` and its mojos
- a new `eclipse-build` mojo that allows to take a literal eclipse project and execute the build on it

#### new `eclipse-build` mojo

The `eclipse-build` mojo can be used like this

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-eclipse-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>eclipse-build</id>
            <goals>
                <goal>eclipse-build</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Support for PDE API Tools annotations

Tycho now supports PDE API Tools annotations to be added to the project automatically.

To enable this add

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-apitools-plugin</artifactId>
    <version>${tycho-version}</version>
</plugin>
```

to your project and make sure it has the `org.eclipse.pde.api.tools.apiAnalysisNature` nature enabled in the `.project` file.
For details how to use these see:

- https://help.eclipse.org/latest/topic/org.eclipse.pde.doc.user/reference/api-tooling/api_javadoc_tags.htm
- https://help.eclipse.org/latest/topic/org.eclipse.pde.doc.user/reference/api/org/eclipse/pde/api/tools/annotations/package-summary.html

### New tycho-repository-plugin

Tycho now contains a new `tycho-repository-plugin` that can be used to package OSGi repositories.

### Referenced repositories are considered by default when resolving the target platform

The option `referencedRepositoryMode` (introduced in Tycho 4.0.2) now defaults to `include`: referenced repositories are considered by default when resolving the target platform, as PDE already does.
To restore the old behavior of Tycho 4.0.2, you need to explicitly set the option to `ignore`:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>target-platform-configuration</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        ... other configuration options ...
        <referencedRepositoryMode>ignore</referencedRepositoryMode>
    </configuration>
</plugin>

```
### New option to filter added repository-references when assembling a p2-repository

If filtering provided artifacts is enabled, the repository references automatically added to a assembled p2-repository
(via `tycho-p2-repository-plugin`'s `addIUTargetRepositoryReferences` or `addPomRepositoryReferences`) can now be filtered by their location
using exclusion and inclusion patterns and therefore allows more fine-grained control which references are added.
Additionally the automatically added references can be filter based on if they provide any of the filtered units or not.
If `addOnlyProviding` is `true` repositories that don't provide any filtered unit are not added to the assembled repo.

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-repository-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        ... other configuration options ...
        <repositoryReferenceFilter>
            <addOnlyProviding>true</addOnlyProviding>
            <exclude>
                <location>https://foo.bar.org/hidden/**</location>
                <location> %regex[http(s)?:\/\/foo\.bar\.org\/secret\/.*]</location>
                <location>![https://foo.bar.org/**]</location>
            </exclude>
        </repositoryReferenceFilter>
    </configuration>
</plugin>

```

### Remove support for deployableFeature option

The deployableFeature option will create "standard eclipse update site directory with feature content will
be created under target folder" but we already removed site-packaging from Tycho for a while, if one wants to
archive similar a category.xml with eclipse-repository packaging gives much more control and power to the user.
Alternatively the new `mirror-target-platform` mojo can be used.

## 4.0.8

backports:
- maven dependencies of transitive projects are not discovered with `-am` / `--also-make`
- Support for new `includeJRE` flag when building products
- Improve SignRepositoryArtifactsMojo handling of unsigned content
- Add demo for pde.bnd integration with classic Eclipse product build
- Use original URL to decide if a IU is provided by reference
- set-version: Fix regression overwriting mismatching versions
- Sort dependency tree root nodes
- Support links in classpath files
- Add support for followRepositoryReference in .target files
- Add flag to fail API tools on resolution error
- Several bugfixes for building pde automatic manifest projects
- Remove special handling of equinox-launcher fragments in AbstractArtifactDependencyWalker

## 4.0.7

backports:
- update to next eclipse release
- tycho-p2-director:director: Fix handling of destination on macOS
- Prevent ConcurrentModificationException in PomInstallableUnitStore
- Add an option to enhance the compile log with baseline problems
- assemble-repository: Prevent sources from being included inadvertently
- ExpandedProduct.getFeatures(ROOT_FEATURES) returns over-qualified IDs
- provide suggested version for features
- Do not fail the DS build if one dependency failed to add
- Add a timestamp provider that inherits the timestamp from the parent
- Add option to include all configured sources in ApiFileGenerationMojo
- Do not fail target resolution if a referenced repository fails
- Add URI to message of GOAWAY info
- Reduce printed warnings in builds

## 4.0.6

### backports:

- Support for CycloneDX Maven Plugin

## 4.0.5

### backports:

- support for parallel execution of product assembly / archiving
- new `repo-to-runnable` mojo
- support for embedded target locations
- using javac as the compiler for Tycho
- new `mirror-target-platform` mojo
- new director mojo
- support for PDE Api Tools Annotations
- api tools fixes
- new `tycho-eclipse-plugin`

## 4.0.4

Backports:
- Add schema-to-html mojo as a replacement for ant ConvertSchemaToHTML
- Only set download/install-size attributes in features if they exist
- Call the API tools directly without using ApiAnalysisApplication
- Make additional P2 units from p2.inf available to the target-platform

## 4.0.3

### New option to filter added repository-references when assembling a p2-repository

If filtering provided artifacts is enabled, the repository references automatically added to a assembled p2-repository
(via `tycho-p2-repository-plugin`'s `addIUTargetRepositoryReferences` or `addPomRepositoryReferences`) can now be filtered by their location
using exclusion and inclusion patterns and therefore allows more fine-grained control which references are added.
Additionally the automatically added references can be filtered based on whether they provide any of the filtered units or not.
If `addOnlyProviding` is `true` then repositories that don't provide any filtered unit are not added to the assembled repository.
```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-repository-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        ... other configuration options ...
        <repositoryReferenceFilter>
            <addOnlyProviding>true</addOnlyProviding>
            <exclude>
                <location>https://foo.bar.org/hidden/**</location>
                <location>%regex[http(s)?:\/\/foo\.bar\.org\/secret\/.*]</location>
                <location>![https://foo.bar.org/**]</location>
            </exclude>
        </repositoryReferenceFilter>
    </configuration>
</plugin>
```

## 4.0.2
- new option to include referenced repositories when resolving the target platform:
Repositories can contain references to other repositories (e.g. to find additional dependencies), from now on there is a new option, `referencedRepositoryMode`, to also consider these references. By default, it is set to `ignore`; to enable referenced repositories in target platform resolution, set it to `include`:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>target-platform-configuration</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        ... other configuration options ...
        <referencedRepositoryMode>include</referencedRepositoryMode>
    </configuration>
</plugin>
```

- Add dummy parameter to prevent warnings with jgit as timestamp provider

## 4.0.1

backports:
- new tycho-repository-plugin
- Non existing but optional dependencies lead to resolving issue in target
- SharedHttpCacheStorage doesn't resolve redirect correctly if the uri that is given isn't normalized
- Non existing but optional dependencies lead to resolving issue in target
- Make comparison of newlines in text files more precise
- Fix resolving of project if target do not contains JUnit
- Check if the about to be injected maven coordinates can be resolved

## 4.0.0

### Maven 3.9 required

Tycho 4.x requires Maven Version 3.9.

### Creating maven p2 sites with Tycho packaging

There is already a way to [create a p2 maven site with Tycho](https://github.com/eclipse-tycho/tycho/blob/master/RELEASE_NOTES.md#create-p2-repository-referencing-maven-artifacts) for plain jar based projects.
This support is now enhanced to being used in a Tycho based setup so it is possible to build a full maven deployed update site automatically with all bundles of the current build.
You can find a demo here:

https://github.com/eclipse-tycho/tycho/tree/master/demo/p2-maven-site


### New document-bundle mojo

There is now a new mojo that replaces the usual Ant-based workflow to generate the help index, it can be used like this:

```xml
<plugin>
    <groupId>org.eclipse.tycho.extras</groupId>
    <artifactId>tycho-document-bundle-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>index</id>
            <goals>
                <goal>build-help-index</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```


### New API-Tools Mojo

There is now a new mojo that replaces the usual ant-based workflow to call the PDE-API tools, it can be used like this:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-apitools-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <baselines>
            <repository>
                <url>... your baseline repo ...</url>
            </repository>
        </baselines>
    </configuration>
    <executions>
        <execution>
            <id>generate</id>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
        <execution>
            <id>verify</id>
            <goals>
                <goal>verify</goal>
            </goals>
            <configuration>
                 <baselines>
                     <repository>
                         <url>${previous-release.baseline}</url>
                     </repository>
                 </baselines>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Building Multi-Release-Jars

Tycho now supports building of [Multi-Release-Jar](https://openjdk.org/jeps/238) in a Manifest-First-Way,
a demo can be found here https://github.com/eclipse-tycho/tycho/tree/master/demo/multi-release-jar

### Building BND Workspace Projects pomless

The tycho-build extension can now also build projects with a [BND Workspaces](https://bndtools.org/concepts.html) layout in a complete pomless way,
details can be found here: https://tycho.eclipseprojects.io/doc/master/BndBuild.html

### Handling of local artifacts can now be configured through the target platform

Previously it was only possible to influence the handling of local artifacts with the `-Dtycho.localArtifacts=<ignore/default>` option, from now on this can be configured through the target platform as well like this:


```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>target-platform-configuration</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <dependency-resolution>
            <localArtifacts>ignore</localArtifacts>
        </dependency-resolution>
    </configuration>
</plugin>
```

the supported values are:

- `include` - (default) local artifacts are included and may override items from the target,
- `default` - for backward-compatibility with older documentation, equivalent to `include`
- `ignore` - local artifacts are ignored

### New tycho-versions-plugin mojos

#### bump-versions mojo

When using version checks it can occur that a version bump is required. This manual and error prone task can now be automated with the `tycho-versions-plugin:bump-versions` mojo, it allows configuration of an automatic version bump behavior in combination with the `org.eclipse.tycho.extras:tycho-p2-extras-plugin:compare-version-with-baselines goal` or similar.

It works the following way:

- You can either configure this in the pom (e.g. in a profile) with an explicit execution, or specify it on the command line like `mvn [other goals and options] org.eclipse.tycho:tycho-versions-plugin:bump-versions`
- If the build fails with a `VersionBumpRequiredException` the projects version is incremented accordingly
- One can now run the build again with the incremented version and verify the automatic applied changes

#### set-property mojo

Updating properties in a project can now be automated with the `tycho-versions-plugin:set-property` mojo. It is very similar to the `tycho-versions-plugin:set-version` mojo but only updates one or more properties, for example:

```shell
mvn org.eclipse.tycho:tycho-versions-plugin:set-property --non-recursive -Dproperties=releaseVersion -DnewReleaseVersion=1.2.3
```

This is mostly useful with [Tycho CI Friendly Versions](https://tycho.eclipseprojects.io/doc/master/TychoCiFriendly.html) where one can define version by properties the mojo can be used to update the defaults.

#### set-parent-version mojo

Updating the parent version in a project can now be automated with the `tycho-versions-plugin:set-parent-version` mojo. Similar to the `tycho-versions-plugin:set-version` mojo, this just updates the version of the parent pom, for example:

```shell
mvn org.eclipse.tycho:tycho-versions-plugin:set-parent-version --non-recursive -DewParentVersion=5.9.3
```

### new bnd-test mojo

Tycho now has a new mojo `tycho-surefire-plugin:bnd-test` to easily execute tests using the [bnd-testing](https://bnd.bndtools.org/chapters/310-testing.html) framework.
This is similar to `tycho-surefire-plugin:plugin-test` but uses the BND testing framework, integrates nicely with the [OSGi Testing Support](https://github.com/osgi/osgi-test)
and allows to execute prebuild test-bundles.

Additional information and a demo can be found here:
https://tycho.eclipseprojects.io/doc/master/TestingBundles.html#bnd-testing

### new tycho-baseline-plugin

Tycho now has a new mojo to perform baseline comparisons similar to the [bnd-baseline-plugin](https://github.com/bndtools/bnd/blob/master/maven/bnd-baseline-maven-plugin/README.md) but takes the tycho-dependency model into account.

A usual configuration looks like this:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-baseline-plugin</artifactId>
    <version>${tycho.version}</version>
    <executions>
        <execution>
            <id>baseline-check</id>
            <goals>
                <goal>verify</goal>
            </goals>
            <configuration>
                <baselines>
                    <repository>
                        <id>optional, only required for proxy setup or password protected sites</id>
                        <url>URL of P2 repository that should be used as a baseline</url>
                    </repository>
                </baselines>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Any baseline problems will then be reported to the build:

![grafik](https://user-images.githubusercontent.com/1331477/205283998-484c6a13-0a66-4b34-9386-599a27bff53e.png)

Also feature baselining is supported according to [Versioning features](https://wiki.eclipse.org/Version_Numbering#Versioning_features)

![grafik](https://user-images.githubusercontent.com/1331477/206921380-5c66cc4b-bf98-4bde-9a95-994d5c9f2a09.png)


### Class loading changes for Eclipse based tests

Due to reported class loading clashes, the ordering of class loading has been modified in Eclipse based tests.
The previous loading can be restored by a new `classLoaderOrder` parameter.
This applies to `tycho-surefire-plugin:test` and `tycho-surefire-plugin:plugin-test`.


### Define targets in repository section

From now on one can define targets also in the repository section of the pom, only the URI variant is supported, but actually you can write everything as an URI already, this then looks like this:

```xml
<project ...>
    ...
    <repositories>
        <repository>
            <id>jetty</id>
            <layout>target</layout>
            <url>file:${project.basedir}/jetty.target</url>
        </repository>
    </repositories>
    ...
</project>
```

You might also use https:

```xml
<url>https://git.eclipse.org/c/lsp4e/lsp4e.git/plain/target-platforms/target-platform-latest/target-platform-latest.target</url>
```

or reference a maven deployed artifact

```xml
<url>mvn:org.eclipse.lsp4e:target-platform-latest:0.13.1-SNAPSHOT</url>
```

or anything that can be resolved to a valid URL in your running build.


### Parameter enhancements for tycho-apitools-plugin:generate goal

The parameters of the `tycho-apitools-plugin:generate` goal have been completed and improved.

### New parameter for tycho-p2-repository-plugin:assemble-repository

The `tycho-p2-repository-plugin:assemble-repository` mojo has now a new configuration parameter `filterProvided` that (if enabled) filter units and artifacts that are already present in one of the referenced repositories.
That way one can prevent including items that are already present in the same form in another repository.

If you want to include repository references automatically, there are two other new options:

- `addPomRepositoryReferences` - all P2 repositories from the pom are added as a reference
- `addIUTargetRepositoryReferences` - all P2 repositories defined in target files IU-location types are added as a reference

so now one can produce a self-contained update-site that only includes what is not already available from the target content used by specify:


```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-repository-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <includeAllDependencies>true</includeAllDependencies>
        <filterProvided>true</filterProvided>
        <addPomRepositoryReferences>true</addPomRepositoryReferences>
        <addIUTargetRepositoryReferences>true</addIUTargetRepositoryReferences>
    </configuration>
</plugin>

```

### Building OSGi Repositories with tycho-p2-repository-plugin:assemble-repository

OSGi defines an own [repository serialization format](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.repository.html) Tycho can now produce such repositories to ease integration with these format, the only thing required is specifying the following configuration options:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-repository-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <generateOSGiRepository>true</generateOSGiRepository>
    </configuration>
</plugin>
```

This will generate an additional `repository.xml` file in the root of the produced p2 repository representing the content as an OSGi Repository.

### New parameter in tycho-packaging-plugin:package-plugin

The `tycho-packaging-plugin:package-plugin` mojo has now a new configuration parameter `deriveHeaderFromSource` (default true), that allows Tycho to discover additional headers declared in the source (e.g. from annotations).
The following headers are currently supported:

- `Require-Capability` is enhanced with additional items, if osgi.ee capability is found, it replaces the deprecated Bundle-RequiredExecutionEnvironment

This can be disabled with the following configuration in the pom:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-packaging-plugin</artifactId>
    <configuration>
        <deriveHeaderFromSource>false</deriveHeaderFromSource>
    </configuration>
</plugin>
```

### Variable resolution in target repository location

URI in `<repository location="...">` in `*.target` files can contain:
- Environment variable as `${env_var:MY_VARIABLE}`
- System variable as `${system_property:myProp}` passed at build time as `-DmyProp`
- Project location as `${project_loc:ProjectName}`

### Migration guide from 3.x to 4.x

### New delayed target platform resolving

Tycho has already introduced a new mode in Tycho 3.0.0 that was activated with `-Dtycho.resolver.classic=false` that was finalized in Tycho 4.x this new mode has several advantages:
- resolve dependencies is delayed until the project is build, this allows more parallelization and even make Tycho start the build faster
- pom dependencies are considered by default, this behaves more like one would expect from a maven perspective
- mixed reactor builds are now fully supported without any special configuration
- Tycho detects if the current project requires dependency resolution and skip resolving the target platform if not needed

If you see any issues please let us know so we can fix any problem with it, this new mode is now configured through the target platform configuration
and if you like the old behavior it can be configured in the following way:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>target-platform-configuration</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <requireEagerResolve>true</requireEagerResolve>
    </configuration>
</plugin>
```

### Tycho-Build Extension uses smart builder

The Tycho-Build Extension now uses the [Takari Smart Builder](https://github.com/takari/takari-smart-builder) which has superior parallel performance when executing with `-T`.

To retain the maven default, or if you even want to use a different builder, you can pass `-Dtycho.build.smartbuilder=false` to your build.


#### skipExec parameter remove from test mojos

The test-mojo previously defined a (deprecated) `skipExec` parameter, this parameter is now removed and one should use `skipTests` instead.

#### allowConflictingDependencies parameter removed

The parameter `allowConflictingDependencies` was removed from the target platform configuration. It solely relied on deprecated and outdated stuff, if there are still use-cases please let us know about them so we can provide alternative ways to archive this.

#### Choosable HTTP transports

Tycho uses a custom P2 transport to download items from updatesites, previously URLConnection was used for this but now the Java HTTP 11 Client is the new default because it supports HTTP/2 now.
To use the old URLConnection one can pass `-Dtycho.p2.httptransport.type=JavaUrl` to the build.

Valid values are:
- `JavaUrl` - uses URLConnection to retrieve files
- `Java11Client` - uses Java 11 HttpClient

#### PGP Signing Enhancements

The [tycho-gpg::3.0.0:sign-p2-artifacts](https://tycho.eclipseprojects.io/doc/3.0.0/tycho-gpg-plugin/sign-p2-artifacts-mojo.html) mojo has been significantly enhanced.

The following properties have been added:

 - `skipIfJarsignedAndAnchored` - This is similar to `skipIfJarsigned` but is weaker in the sense that the signatures are checked in detail such that the PGP signing is skipped if and only if one of the signatures is anchored in Java cacerts.  The default is `false`. Set `skipIfJarsignedAndAnchored` to `true` and `skipIfJarsigned` to `false` to  enable this feature.
  - `skipBinaries` - Setting this to `false` will enable the signing of binary artifacts, which are of course not jar-signed.
  - `pgpKeyBehavior` - Specify `skip`, `replace`, or `merge` for how to handle the signing of artifacts that are already PGP signed.
  - `signer` - Currently supported are `bc` and `gpg` where the former is a new implementation that uses Bouncy Castle for signing, which is significantly faster and allows signing to proceed in parallel. This can also be configured by the system property `tycho.pgp.signer`.

#### mixed reactor setups require the new resolver now

If you want to use so called mixed-reactor setups, that is you have bundles build by other techniques than Tycho (e.g. bnd/felix-maven-plugin) mixed with ones build by Tycho,
previously this was only possible with enabling an incomplete resolver mode and using `pomDependencies=consider`.

From now on such setups require the use of the new resolver mode (`-Dtycho.resolver.classic=false`) supporting the usual resolver mode and thus incomplete resolver mode was removed completely.

#### pom declared dependencies handling has slightly changed

With the new resolver mode (`-Dtycho.resolver.classic=false`) pom dependencies are considered by default, also the way how they are handled have slightly changed:

Previously all units where always added to the full target resolution result. This has often lead to undesired effects, especially when there are large (transitive) dependency chains
as things can easily slip in.

From now on the target platform is always queried first for a unit fulfilling the requirement and only if not found the pom dependencies are queried for an alternative.

#### pom declared dependencies are considered for compile

Previously dependencies declared in the pom are ignored by Tycho completely and even though one could enable these to be considered in target platform
this still requires them to be imported in the bundle manifest to finally be usable for compilation.

Now each pom defined dependency is always considered for compilation as this matches the expectation of most maven users and finally allows to have 'compile only' dependencies to be used,
for example with annotations that are only retained at source or class level.

One example that uses [API-Guardian](https://github.com/apiguardian-team/apiguardian) annotations can be found here: https://github.com/eclipse/tycho/tree/master/tycho-its/projects/compiler-pomdependencies

You can disable this feature through the `tycho-compiler-plugin` configuration:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-compiler-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <pomOnlyDependencies>ignore</pomOnlyDependencies>
    </configuration>
</plugin>
```

#### Properties for tycho-surefire-plugin's 'useUIThread' and 'useUIHarness' parameters

The configuration parameters `useUIThread` and `useUIHarness` parameter of the `tycho-surefire-plugin` can now be set via the properties `tycho.surefire.useUIHarness` respectively `tycho.surefire.useUIThread`.

#### Minimum version for running integration/plugin tests

Previously the `osgibooter` has claimed to be Java 1.5 compatible but as such old JVMs are hard to find/test against already some newer code was slipping in. It was therefore decided to raise the minimum requirement to Java 1.8 what implicitly makes it the lowest bound for running integration/plugin tests with Tycho.

This requires any tests using pre 1.8 java jvm to be upgrade to at least running on Java 1.8.

#### Using integration/plugin tests with eclipse-plugin packaging

Some improvements have been made for the test execution with `eclipse-plugin` packaging that probably needs some adjustments to your pom configuration or build scripts:

1. The property `skipITs` has been renamed to `tycho.plugin-test.skip`
2. the mojo `integration-test` has been renamed to `plugin-test`
3. the default pattern of the former `integration-test` has been changed from `**/PluginTest*.class", "**/*IT.class` to the maven default `**/Test*.class", "**/*Test.class", "**/*Tests.class", "**/*TestCase.class`
4. the former `integration-test` mojo is no longer part of the default life-cycle, that means it has to be explicitly be enabled to be more flexible and this is how standard maven behaves
5. the `test` mojo of the `maven-surefire-plugin` is no longer part of the default life-cycle, that means it has to be explicitly be enabled to be more flexible and to not pollute the test-phase.

To restore old behaviour you can add the follwoing snippet to your (master) pom:

```xml
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

<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-surefire-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>execute-plugin-tests</id>
            <configuration>
                <includes>**/PluginTest*.class,**/*IT.class</includes>
            </configuration>
            <goals>
                <goal>plugin-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### New Maven dependency consistency check

Tycho has a new mojo to check the consistency of the pom used for your bundle.
To enable this add the following to your pom (or adjust an existing configuration):

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-packaging-plugin</artifactId>
    <executions>
        <execution>
            <id>validate-pom</id>
            <phase>verify</phase>
            <goals>
                <goal>verify-osgi-pom</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <archive>
            <addMavenDescriptor>true</addMavenDescriptor>
        </archive>
        <mapP2Dependencies>true</mapP2Dependencies>
    </configuration>
</plugin>
```
This will then:

1. add a new execution of the `verify-osgi-pom` mojo
2. enable the generation and embedding of a maven descriptor (optional if you fully manage your pom.xml with all dependencies)
3. map P2 dependencies to maven dependencies (optional, but most likely required to get good results)

### Default value change for trimStackTrace

tycho-surefire-plugin had set the default value of the trimStackTrace option to true.
The default will now be aligned with maven-surefire-plugin at false and will need to be manually adjusted for users that really need the stack traces trimmed.

Old behavior can be restored through configuration of the tycho-surefire-plugin:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-surefire-plugin</artifactId>
    <configuration>
        <trimStackTrace>true</trimStackTrace>
    </configuration>
</plugin>
```

## 3.0.5

### Backports

- inject source folders into maven model
- improve CI friendly versions
- Report download speed to the caller
- redirectTestOutputToFile for OsgiSurefireBooter

## 3.0.4

### Backports

- Include smartbuilder (but disabled by default), can be enabled with `-Dtycho.build.smartbuilder=true`
- tycho-bnd-plugin support
- Version Mojo Bugfixes

## 3.0.3

### Dependency upgrades and Maven 3.9.0 support

This release includes some dependency upgrades and a fix to run Tycho 3 with Maven 3.9.0.

### Parameter enhancements for tycho-apitools-plugin:generate goal

The parameters of the `tycho-apitools-plugin:generate` goal have been completed and improved.

## 3.0.2

### Fixed support for the generation of a source feature from a maven target-location template

The generated source feature now properly includes the source bundles.

### EclipseRunMojo `argLine` and `appArgLine` are reintroduced and no longer deprecated.

The `argLine` and `appArgLine` options have long been deprecated and were removed in Tycho 3.0.0.
They are generally inferior to the list-based `jvmArgs` and `applicationArgs` respectively.
However there are use cases where the arguments need to be extensible via property expansion, in which case the list-based approach is not always a suitable alternative.
As such, these two options have been re-introduced for Tycho 3.0.2 and are no longer marked deprecated though `jvmArgs` and `applicationArgs` remain the preferred mechanism.

### Backports
- Maven Loockup can become really slow and should cache previous requests #1969
- Provide a "verify-pom-resolves" mojo #1451
- JUnit 5.9 support in Tycho 3.0.x #1943
- Consumer-POM should use packaging-type jar instead of eclipse-plugin #2005
- Mirroring of packed artifacts must be disabled
- Target platform resolved multiple times
- Support resolving of target projects from the reactor

## 3.0.1

### Fix for java.lang.NoSuchMethodError: 'void org.eclipse.equinox.internal.p2.repository.helpers.ChecksumProducer

If you face the following error with using Tycho 3.0.0 update to 3.0.1 to fix it.

```
[ERROR] Failed to execute goal org.eclipse.tycho:tycho-p2-repository-plugin:3.0.0:assemble-repository (default-assemble-repository) on project XXXX: Execution default-assemble-repository of goal org.eclipse.tycho:tycho-p2-repository-plugin:3.0.0:assemble-repository failed: An API incompatibility was encountered while executing org.eclipse.tycho:tycho-p2-repository-plugin:3.0.0:assemble-repository: java.lang.NoSuchMethodError: 'void org.eclipse.equinox.internal.p2.repository.helpers.ChecksumProducer.<init>(java.lang.String, java.lang.String, java.lang.String)'
```





## 3.0.0

### Tycho now support forking of the Eclipse Java Compiler

Previously forking was not supported, now forking is possible and will be used if a custom java home is specified.

### New option to transform P2 dependencies into real maven coordinates

The `tycho-consumer-pom` mojo has a new option to resolve p2 introduced dependencies to 'real' maven coordinates now, when enabled it queries maven-central with the SHA1 of the file to find out what are the actual maven central coordinates
 and place them in the generated pom consumer-pom.

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-packaging-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <mapP2Dependencies>true</mapP2Dependencies>
    </configuration>
</plugin>
```

### New tycho-p2-plugin:dependency-tree mojo

Sometimes it is useful to find out how dependencies of a project are actually pulled in. Tycho now supports a new `tycho-p2-plugin:dependency-tree` mojo that outputs a tree view of the P2 dependecies of a tycho project.

Example with Tycho integration test project:

```shell
tycho-its/projects/reactor.makeBehaviour$ mvn org.eclipse.tycho:tycho-p2-plugin:3.0.0-SNAPSHOT:dependency-tree

...

[INFO] --- tycho-p2-plugin:3.0.0-SNAPSHOT:dependency-tree (default-cli) @ feature2 ---
[INFO] tycho-its-project.reactor.makeBehaviour:feature2:eclipse-feature:1.0.0-SNAPSHOT
[INFO] +- feature2.feature.group (1.0.0.qualifier) --> [tycho-its-project.reactor.makeBehaviour:feature2:eclipse-feature:1.0.0-SNAPSHOT]
[INFO]    +- bundle2 (1.0.0.qualifier) satisfies org.eclipse.equinox.p2.iu; bundle2 0.0.0 --> [tycho-its-project.reactor.makeBehaviour:bundle2:eclipse-plugin:1.0.0-SNAPSHOT]
[INFO]    +- feature1.feature.group (1.0.0.qualifier) satisfies org.eclipse.equinox.p2.iu; feature1.feature.group 0.0.0 --> [tycho-its-project.reactor.makeBehaviour:feature1:eclipse-feature:1.0.0-SNAPSHOT]
[INFO]       +- bundle1 (1.0.0.qualifier) satisfies org.eclipse.equinox.p2.iu; bundle1 0.0.0 --> [tycho-its-project.reactor.makeBehaviour:bundle1:eclipse-plugin:1.0.0-SNAPSHOT]
...
```

### Support for inclusion of all source bundles in an update-site

The [tycho-p2-repository-plugin:2.7.0:assemble-repository](https://www.eclipse.org/tycho/sitedocs/tycho-p2/tycho-p2-repository-plugin/assemble-repository-mojo.html) now support a new property `includeAllSources` that,
when enabled, includes any available source bundle in the resulting repository.

### Support for Eclipse-Products with mixed Features and Plugins

Tycho now supports building _mixed_ Products. In mixed Products both the listed features and listed plug-ins are installed.
Therefore the Product attribute `type` is now supported, which can have the values `bundles`, `features` and `mixed` and takes precedence over the boolean-valued `useFeatures` attribute.

### New API Tools Mojo

Tycho now provides a new API Tools Mojo, see https://github.com/eclipse/tycho/tree/master/tycho-its/projects/api-tools for an example how to use it.

### new sisu-osgi-connect

The new sisu-osgi-connect provides an implementation for plexus according to the [Connect Specification](http://docs.osgi.org/specification/osgi.core/8.0.0/framework.connect.html#framework.connect) that allows to run an embedded OSGi Framework from the classpath of a maven-plugin.
As both, the maven plugin and the embedded framework, share the same classlaoder you can use the best of both worlds and interact seamless with them.

This can be used in the following way:

```java
@Component(role = MyPlexusComponent.class)
public class MyPlexusComponent {
    @Requirement(hint = "connect")
    private EquinoxServiceFactory serviceFactory;

    public void helloConnect() {
        serviceFactory.getService(HelloWorldService.class).sayHello();
    }
}
```

For the setup you need to do the following:

1. include any bundle you like to make up your plexus-osgi-connect framework as a dependency of your maven plugin
2. include a file `META-INF/sisu/connect.bundles` that list all your bundles you like to have installed in the format `bsn[,true]`, where `bsn` is the symbolic name and optionally you can control if your bundle has to be started or not
3. include the following additional dependency
```xml
<dependency>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>sisu-osgi-connect</artifactId>
    <version>${tycho-version}</version>
</dependency>
```

### Deprecated Features

The `tycho-compiler:compile` and `tycho-compiler:testCompile` option `requireJREPackageImports` is deprecated now and will produce a warning when used, bundles currently rely on this option should migrate to proper importing packages from the non java.* namespace.

### Tycho compiler support for java.* imports

The `tycho-compiler:compile` and `tycho-compiler:testCompile` has a new option `requireJavaPackageImports` (defaults to `false`) that allows to assert importing of packages from the `java.*` namespace.
This is [allowed since OSGi R7](https://blog.osgi.org/2018/02/osgi-r7-highlights-java-9-support.html) and considered  ǵood practice since the evolving of modular VMs there is no guarantee what packages a JVM offers,

### Eclipse M2E lifecycle-mapping metadata embedded

All Tycho plugins are now shipped with embedded M2E lifecycle-mapping-metadata files.
Therefore M2E now knows by default how to handle them and it is not necessary anymore to install any connector (usually `org.sonatype.tycho.m2e` was used) for them.

### Support for BND in tycho-build extension (aka pomless builds)

The Tycho Build Extension (aka pomless build) now detects bnd.bnd files in the root of a pomless bundle and automatically generates an appropriate maven execution automatically.
This can be used to generate any content by the BND plugin, e.g. declarative service xml or JPMS infos see here for an example:

https://github.com/eclipse/tycho/tree/master/tycho-its/projects/pomless/bnd

### Tycho no longer ships JVM profiles

Because of modular VMs the profiles shipped by Tycho has never been complete and actually are already partly generated in regards to available packages.
From now on, Tycho do not ship any profiles and thus you can use any VM in the toolchains or as a running VM and Tycho will generate a profile for it.

### Enhanced Support for Maven CI Friendly Versions

Starting with Maven 3.8.5 Tycho now supports an enhanced form of the [Maven CI Friendly Versions](https://maven.apache.org/maven-ci-friendly.html) beside the standard properties names one could also use:

- releaseVersion
- major
- minor
- micro
- qualifier

These uses the usual semantics that you can use them in a version string e.g. `<version>${releaseVersion}${qualifier}</version>` and pass them on the commandline.

Beside this, Tycho supports some useful default calculation for `qualifier` if you give a format on the commandline with `-Dtycho.buildqualifier.format=yyyyMMddHHmm`
(or [any other format supported](https://www.eclipse.org/tycho/sitedocs/tycho-packaging-plugin/build-qualifier-mojo.html#format)). Tycho will also make the build qualifier available in your Maven model!

That way you can configure your pom in the following way:
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>...</groupId>
    <artifactId>...</artifactId>
    <packaging>pom</packaging>
    <version>1.0.0${qualifier}</version>
    <properties>
        <!-- Defines the default Qualifier if no format is given-->
        <qualifier>-SNAPSHOT</qualifier>
        ...
    </properties>
    ...
</project>
```

What will result in the usual `1.0.0-SNAPSHOT` for a regular `mvn clean install`, if you want to do a release, you can now simply call `mvn -Dtycho.buildqualifier.format=yyyyMMddHHmm clean deploy`
and your artifact will get the `1.0.0-<formatted qualifier>` version when published! This also is supported if you use pomless build.

To use this new feature you need to enable the tycho-build extension with the `.mvn/extensions.xml` file in the root of your project directory:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>org.eclipse.tycho</groupId>
        <artifactId>tycho-build</artifactId>
        <version>${tycho-version}</version>
    </extension>
    <!-- possibly other extensions here -->
</extensions>
```
Please note that we use another new feature from Maven 3.8.5 here, where you can use properties from the file `.mvn/maven.config` in your `.mvn/extensions.xml` file, so if you put in this:
```shell
-Dtycho-version=3.0.0-SNAPSHOT
(probably add more here ...)
```

You can now control your Tycho version for `.mvn/extensions.xml` and your `pom.xml` in one place and still override it on the commandline with `-Dtycho-version=...`


### Support for non-modular JVMs no longer top tier

Support for compilation for pre-Java 11 JVMs bytecode is no longer considered first class nor tested. Actual support is not removed but people facing issues with it will have to come with fixes on their own.

### Support for PDE Declarative Component Annotation processing

One can enable either global or per project the generation of component xmls in PDE. Until now it was required for Tycho to still import the annotation package even though `classpath=true` was set, beside that one needs to check in the generated xmls.

Tycho now has improved support for this with the following:

1. if there is a `.settings/org.eclipse.pde.ds.annotations.prefs` in the project, tycho adapts the settings there and if `classpath=true` is set no more imports are required.
2. one can enable a new `tycho-ds-plugin` where global default settings can be configured if project settings are not present, the below shows an example with default values:
```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-ds-plugin</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <classpath>true</classpath>
        <dsVersion>1.3</dsVersion>
        <enabled>false</enabled>
        <path>OSGI-INF</path>
        <skip>false</skip>
    </configuration>
</plugin>
```
If the `tycho-ds-plugin` is enabled for a project it generates the necessary xml files if not already present in the project.

### Improved P2 transport for more efficiently http-cache handling and improved offline mode

P2 default transport is more designed as a weak cache that assumes the user is always online.
While for an IDE that might be sufficient as target resolution is only performed once in a while and updates are triggered by explicit user request, for Tycho this does not work well:

- Builds are often trigger on each code change, requiring repeated target resolution.
- Builds might be asked to run in an offline mode.
- If there is a temporary server outage one might want to fall back to the previous state for this build instead of failing completely.
- Build times are often a rare resource one doesn't want to waste waiting for servers, bandwidth might even be limited or you have to pay for it.

Because of this, Tycho now includes a brand new caching P2 transport that allows advanced caching, offline handling and fallback to cache in case of server failures. The transport is enabled by default so nothing has to be done, just in case you want the old behavior you can set `-Dtycho.p2.transport=ecf` beside that the following properties might be interesting:

#### Force cache-revalidation

If you run maven with the `-U` switch Tycho revalidates the cache.
This is useful if you have changed an updatesite in an unusual way (e.g. adding new index files) as tycho now also caches not found items to speed-up certain scenarios where many non existing files are queried.

#### Configure minimum caching age

Some servers don't provide you with sufficient caching information. For this purpose, Tycho by default assumes a minimum caching age of one hour. You can switch this off, or configure a longer delay by using `-Dtycho.p2.transport.min-cache-minutes=<desired minimum in minutes>`.
Choosing a sensible value could greatly improve your build times and lower bandwith usage.
If your build contains a mixture of released and 'snapshot' sites you have the following options:

1. Consider adding a mirror to your settings.xml for the snapshot page that points to a file-local copy (e.g. output of another build).
2. Configure the webserver of your snapshot site with the `Cache-Control: must-revalidate` header in which case Tycho ignores any minimum age.
3. Use `-Dtycho.p2.transport.min-cache-minutes=0`. This will still improve the time to resolve the target.


### Automatic generation of PDE source bundles for pom-first bundles

PDE requires some special headers to detect a bundle as a "Source Bundle", there is now a new mojo `tycho-source-plugin:generate-pde-source-header` that supports this, it requires the following configuration:

1. Enable the generation of a source-jar with the `maven-source-plugin`. Please note that it needs to be bound to the `prepare-package` phase explicitly!
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-source-plugin</artifactId>
    <executions>
        <execution>
            <id>attach-sources</id>
            <goals>
                <goal>jar</goal>
            </goals>
            <phase>prepare-package</phase>
        </execution>
    </executions>
</plugin>
```
2. Enable the generation of the appropriate PDE header:
```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-source-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>generate-pde-source-header</id>
            <goals>
                <goal>generate-pde-source-header</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
3. Finally enable generation of P2 metadata so Tycho can use the source bundle in the build (you can omit this step if you don't want to reference the source bundle in a product, update-site or feature).
```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-p2-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
            <id>attached-p2-metadata</id>
            <phase>package</phase>
            <goals>
                <goal>p2-metadata</goal>
            </goals>
            <configuration>
                <supportedProjectTypes>
                    <value>bundle</value>
                    <value>jar</value>
                </supportedProjectTypes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Limit the number of parallel test executions across the reactor

You can specify a `<reactorConcurrencyLevel>` (default unlimited) for `tycho-surefire:integration-test` and `tycho-surefire:test` that limits the number of concurrently running tests.
This can be useful if you like to execute the build with multiple threads (e.g. `-T1C`) but want to run the integration tests in a serial manner (e.g. because they are UI based).

### Migration guide 2.x -> 3.x

#### Java 17 required as runtime VM

From 3.x on Tycho requires Java 17 as a runtime VM, but you can still compile code for older releases.

#### Default value for archive-products has changed

Previously Tycho uses `zip` for all platforms when packaging a product, now `.tar.gz` is used for linux+mac. If you want you can restore old behaviour by:

```xml
<execution>
    <id>archive-products</id>
    <goals>
        <goal>archive-products</goal>
    </goals>
    <phase>install</phase>
    <configuration>
        <formats>
            <linux>zip</linux>
            <macosx>zip</macosx>
        </formats>
    </configuration>
</execution>

```

#### Remove `tycho.pomless.testbundle` switch from `build.properties` in favor of specification of project's packaging-type

The boolean property `tycho.pomless.testbundle`, which allowed to specify in the `buid.properties` if a Plug-in is a Test-Plugin or not, has been removed.
Instead one can specify the packaging-type of that Maven-project directly. To migrate simply apply the following replacements in the `build.properties`:<br>
`tycho.pomless.testbundle = true` -> `pom.model.packaging = eclipse-test-plugin`<br>
`tycho.pomless.testbundle = false` ->`pom.model.packaging = eclipse-plugin`

This already works in the Tycho 2.7.x stream (but the generated artifactId does not yet have a 'test'-prefix).

#### sisu-equinox is now sisu-osgi

The sisu-equinox module is now cleaned up and made more generic so it could be used in a wider area of use case, therefore the equinox part is stripped and some API enhancements are performed.
As sisu-equinox is a separate module used inside Tycho, users of Tycho itself are usually not affected, but plugin developers might need to adjust there code to conform to the changed API contracts.

#### publish-osgi-ee do not publish a fixed size of profiles anymore

The `publish-osgi-ee` previously has published a fixes list of "usefull" execution environments maintained by Tycho.
This is no longer true and Tycho do publish only those JavaSE profiles that are available to the build and have a version larger than 11 if not configured explicitly.

#### jgit-timestamp provider moved from `org.eclipse.tycho.extras` to `org.eclipse.tycho`

The `tycho-buildtimestamp-jgit` plugin has moved to the `org.eclipse.tycho` group id.

#### Removal of deprecated `eclipse-update-site` and `eclipse-application` packaging types

These packaging types have been deprecated since a long time and there is the replacement `eclipse-repository`.

#### Removal of `tycho-pomgenerator:generate-poms`

The `tycho-pomgenerator:generate-poms` mojo is no longer supported as it was useful only in the days where tycho-pomless was incomplete.
Today its usage is very limited and tycho-pomless is a much better (and supported) alternative now.

#### Removal of `tycho-source-feature:source-feature`

This mojo is replaced by the `tycho-source-plugin` with execution `feature-source` which offers equivalent and even enhanced functionality.

#### Pack200

Pack200 technology is obsolete and no longer supported after Java 13.
Tycho removed all support for dealing with pack.gz files including pack200 specific plugins, various options in Mojos and support for resolving fetching in core.

Omit any pack200 specific plugins, options and in any other way dealing with ***.pack.gz** files.
The following are removed:
- Plugins
    - tycho-pack200a-plugin
    - tycho-pack200b-plugin
- Mojo options
    - TargetPlatformConfigurationMojo' `includePackedArtifacts`
    - MirrorMojo's `includePacked`
    - PublishFeaturesAndBundlesMojo `reusePack200Files`

#### BuildQualifierMojo `project.basedir` option removed

It was totally ignored in all latest versions.

#### PublishProductMojo `flavor` option removed

It was hardcoded to "tooling" always and had no practical meaning to change.

#### EclipseRunMojo `argLine` and `appArgLine` options removed / `applicationArgs` option fixed

`argLine` and `appArgLine` have been replaced by list-based `jvmArgs` and `applicationArgs` respectively.

`applicationArgs` (previously known as `applicationsArgs`) has been corrected to not perform any
interpretation of whitepace and quotes anymore. Individual arguments are now used literally (just like `jvmArgs`).

## 2.7.5

Fixes:

- [reverted] Not all (direct) requirements of a feature are considered when building an update-site
- [backport] Fix MavenLocation scope filtering
- org.eclipse.tycho:tycho-packaging-plugin:2.7.3:package-plugin issuing error <<pluginname>>/target/classes does not exist

## 2.7.4

Fixes:
- Tycho reports wrong type in case of maven GAV restored from UI
- Support bnd processing in pomless builds
- The official maven keyserver is just to slow use ubuntu as an alternative mirror first
- [Maven-Target] Consider extra-repositories when fetching source-jars
- DS generation fails with latest eclipse release for UI bundles
- PomDependencyCollector uses a wrong type for pom declared feature dependencies
- Not all (direct) requirements of a feature are considered when building an update-site
- Fix Mojo Configuration of DS Plugin is ignored
- Check that components declared in the manifest exits

### Eclipse M2E lifecycle-mapping metadata embedded

All Tycho plugins are now shipped with embedded M2E lifecycle-mapping-metadata files.
Therefore M2E now knows by default how to handle them and it is not necessary anymore to install any connector (usually `org.sonatype.tycho.m2e` was used) for them.

## 2.7.3

Fixes:
-  p2-maven-site includes bundles in the repository https://github.com/eclipse/tycho/issues/932

## 2.7.2

Fixes:
- [2.7.1] (regression) Neither raw version nor format was specified https://github.com/eclipse/tycho/issues/876
- [2.7.1] 'includePackedArtifacts' must be automatically disabled when running with an incompatible vm https://github.com/eclipse/tycho/issues/885
- Resolve DS classpath entry and generate component xmls https://github.com/eclipse/tycho/issues/406

## 2.7.1

Fixes:
- Access to the Tycho .cache directory is not properly synchronized https://github.com/eclipse/tycho/issues/663
- compare-versions-with-baseline failing (since 2.7) when executionEnvironment=none https://github.com/eclipse/tycho/issues/707
- JGit packaging build fails with Tycho 2.7.0 https://github.com/eclipse/tycho/issues/723
- Backport of https://github.com/eclipse/tycho/issues/767
- Maven artifacts deployed with Tycho 2.7 are resolved without transitive dependencies by Maven https://github.com/eclipse/tycho/issues/781
- Slicer warnings are too verboose https://github.com/eclipse/tycho/issues/728
- Performance regression in classpath resolution https://github.com/eclipse/tycho/issues/719
- If multiple fragments match a bundle all items are added to the classpath while only the one with the highest version should match https://github.com/eclipse/tycho/issues/822
- Check Hashsums for local cached artifacts https://github.com/eclipse/tycho/issues/692
- JAVA_HOME check is not OS independent https://github.com/eclipse/tycho/issues/849
- Bug 571533 - tycho-compiler-plugin with useJDK=BREE and BREE==JavaSE-1.8 fails to find some EE packages https://github.com/eclipse/tycho/issues/51
- Failed to resolve dependencies with Tycho 2.7.0 for custom repositories https://github.com/eclipse/tycho/issues/697
- Feature restrictions are not taken into account when using emptyVersion https://github.com/eclipse/tycho/issues/845

## 2.7.0

### Tycho-Pomless will become a tycho-core extension

Tycho pomless has started as a small experiment in tycho-extras. Over time it has grown to a fully-fledged solution to build pde-based artifacts with less effort and nearly zero additional configuration.

Neverless, the name "pomless" was always a bit misleading, as actually we have reduced the number required poms to one 'main-pom' it is still not pomless and actually allows poms to be used where suitable.
Because of this, and to not limit the usage to "pomless" with this version a new core-extension is available name 'tycho-build', that effectively does what tycho-extras-pomless does but in the context of 'core' and is open to further improvements (maybe at some time offering an option to not needing a pom at all).

All that needs to be done is to replace the old
```xml
<extension>
    <groupId>org.eclipse.tycho.extras</groupId>
    <artifactId>tycho-pomless</artifactId>
    <version>2.7.0</version>
</extension>
```

with

```xml
<extension>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-build</artifactId>
    <version>2.7.0</version>
</extension>
```
Notice the changed artifactId.

### Tycho-specific Maven GraphBuilder to support `--also-make` (`-am`) and `--also-make-dependents` (`-amd`)

The `tycho-build` extension (see above) was updated with a [custom `org.apache.maven.graph.GraphBuilder` implementation](https://github.com/eclipse/tycho/pull/577).

Without this, the Maven options `--also-make` (`-am`) and `--also-make-dependents` (`-amd`) were not supported in a Tycho-based build, since only pom-dependencies were considered by Maven.

Using the custom `GraphBuilder` Tycho is able to perform P2 dependency resolution early-on and supply Maven with an updated set of projects required for the build.

### Mixed reactor build support

Previously Tycho has resolved pom considered dependencies as part of the initial Maven setup (before the actual build starts). This has led to the fact that it was not possible to mix projects that e.g. dynamically generate a manifest.

This was [now changed](https://github.com/eclipse/tycho/issues/462) and Tycho can now build mixed project setups. See this integration test as an example:
https://github.com/eclipse/tycho/tree/master/tycho-its/projects/mixed.reactor

This slightly changes some of the behavior of previous `pomDependencies=consider`:

- dependencies of pom considered items always have to be declared on the Maven level (either by the project using it or the dependency declaring it)
- pom considered items do not participate in the build-order computation as of the previous statement already ensure this
- if enabled, builds might fail later as projects are allowed to have incomplete requirements up until the `initialize` phase.

There is one restriction for such mixed setups, see: https://github.com/eclipse/tycho/issues/479

### Caution when switching between Tycho versions

Tycho 2.7 changed how it handles bad p2 maven meta-data. When you run builds with Tycho 2.7+ and then run builds with older Tycho versions, the meta-data written to your local Maven repository (e.g. ~/.m2/repository/.meta/p2-artifacts.properties) by Tycho 2.7 confuses older Tycho versions, so using different local Maven repositories or deleting that file between builds is recommended when switching between builds with current Tycho and older versions.

## 2.6.0

### Delayed classpath computation
Previously the classpath of a project was computed in the maven-setup phase, this [has several restrictions](https://github.com/eclipse/tycho/issues/460).
Tycho now delays the classpath computation to a later stage (`initialize` phase).

If you want to perform the classpath validation in the `validate` phase of your build you can force classpath computation with the following snippet:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-compiler-plugin</artifactId>
    <version>${tycho.version}</version>
    <executions>
        <execution>
            <id>verify-classpath</id>
            <phase>validate</phase>
            <goals>
                <goal>validate-classpath</goal>
            </goals>
            <configuration>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Support for generation of a feature from a maven target-location template
Tycho now supports the m2e feature to [generate a feature from a maven target location](https://github.com/eclipse-m2e/m2e-core/blob/master/RELEASE_NOTES.md#the-m2e-pde-editor-now-supports-generation-of-a-feature-from-a-location).

### Support for nested targets
Tycho now supports [nested target locations](https://github.com/eclipse/tycho/issues/401).

An example can be found [here](https://github.com/eclipse/tycho/tree/master/tycho-its/projects/target.references/target.refs).

### Support for pom dependencies in maven target locations
Tycho now supports [pom dependencies inside maven target locations](https://github.com/eclipse/tycho/issues/331).

Example:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<?pde version="3.8"?>
<target name="with-pom-dependency">
   <locations>
      <location includeSource="true" missingManifest="generate" type="Maven">
         <dependencies>
            <dependency>
               <groupId>com.sun.xml.ws</groupId>
               <artifactId>jaxws-ri</artifactId>
               <version>3.0.2</version>
               <type>pom</type>
            </dependency>
         </dependencies>
      </location>
   </locations>
</target>
```

### Mirror Mojo no longer mirrors pack200 artifacts by default

The default for this mojo has been flipped from true to false as pack200 artifacts are irrelevant nowadays. If you want to restore previous behavior put the following into your mojo configuration:

```xml
<includePacked>true</includePacked>
```

### Improved plain JUnit 5 support

Plain JUnit 5 tests now work fine without an extra dependency on JUnit 4.

### Parallel testing with Tycho Surefire

Previously, Tycho Surefire would only execute one test plugin at the same time, even with parallel Maven builds enabled. Now Tycho Surefire [runs multiple tests in parallel](https://github.com/eclipse/tycho/issues/342). If you have parallel Maven builds enabled and run SWTBot UI tests (or other tests that don't work well during parallel execution), then you may need to re-configure your build to avoid the parallel test execution.

### Javadoc generation can use JAVA_HOME

The [Tycho Extras document-bundle-plugin](https://www.eclipse.org/tycho/sitedocs/tycho-extras/tycho-document-bundle-plugin/plugin-info.html) now supports looking up the javadoc executable in the [path defined by the JAVA_HOME environment variable](https://github.com/eclipse/tycho/issues/471), in addition to other supported locations.

## 2.5.0

### [Support for PGP Signatures in maven-p2 sites](https://github.com/eclipse/tycho/issues/203)

The `assemble-maven-repository` mojo now supports embedding the PGP signature of maven artifacts to allow additional verifications and trust decisions.

### Support for new m2e-pde features

Tycho supports the new m2e-pde features regarding [multiple dependencies per target](https://github.com/eclipse-m2e/m2e-core/blob/master/RELEASE_NOTES.md#the-m2e-pde-editor-now-supports-adding-more-than-one-dependency-per-target-location) and specifying [extra repositories in the target](https://github.com/eclipse-m2e/m2e-core/blob/master/RELEASE_NOTES.md#the-m2e-pde-editor-now-supports-adding-additional-maven-repoistories-for-a-target-location).

### [Improved cache handling](https://github.com/eclipse/tycho/pull/211)

Previously Tycho failed if the remote update-site server was not available even if the file is already downloaded. Now, it uses the local file instead and issues a warning instead.

### [M2_REPO classpath variable support](https://github.com/eclipse/tycho/pull/207)

Tycho now supports M2_REPO variable from .classpath


## 2.4.0

### [Support resolving of JUnit Classpath Container](https://bugs.eclipse.org/bugs/show_bug.cgi?id=572602)
It is now possible to resolve the JDT 'JUnit Classpath Container' (this is meant only for `eclipse-plugin` projects, NOT for `eclipse-test-plugin`), for this do the following:

- add the 'JUnit Classpath Container' to the classpath of your eclipse project
- make sure you check in the .classpath file
- Now you can use the Junit classes without explcitly adding them to your bundle via require-bundle/import package

For an example take a look at the [integration tests](https://github.com/eclipse/tycho/tree/master/tycho-its/projects/compiler.junitcontainer/junit4-in-bundle)

### [Execute unit-tests with eclipse-plugin packaging](https://bugs.eclipse.org/bugs/show_bug.cgi?id=572420)
Previously, unit-tests were only executed for `eclipse-test-plugin` packaging types. Besides that, it was only possible to execute them as part of the **tycho-surefire:test** goal (which executes them inside an OSGi runtime) in the `integration-test` phase (making them actually some kind of integration tests).

From now on, this restriction is no longer true and one is able to execute unit-tests with **maven-surefire** as well as integration-tests with the **tycho-surefire:integration-test** goal (which still executes them inside an OSGi runtime, see below). This works the following way:

 - create a source-folder in your `eclipse-plugin` packaged project and marks it as "Contains test sources" in the classpath settings:![grafik](https://user-images.githubusercontent.com/1331477/116801917-b20cb080-ab0e-11eb-8c05-1796196ccb25.png)
 - Create a unit-test inside that folder, either name it with any of the [default-pattern](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#includes) maven-surefire plugin or configure the include pattern explicitly.
 - Include maven-surefire plugin configuration in your pom to select the appropriate test-providers
```xml
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

As said above, Tycho also includes a new **tycho-surefire:integration-test** goal, that is similar to the **tycho-surefire:test** one:
 - it executes in the `integration-test` phase, but does not fail the build if a test fails, instead a summary file is written
 - the outcome of the tests are checked in the `verify` phase (and the build fails at that point, if necessary)
 - this allows to hook some setup/teardown mojos (e.g. start webservers, ...) in the `pre-integration-test` phase and to safely tear them down in the `post-integration` phase (thus the name 'failsafe' see [maven-failsafe-plugin FAQ](https://maven.apache.org/surefire/maven-failsafe-plugin/faq.html) for some more details.

Given you have the above setup you create an integration-test (executed in an OSGi runtime like the traditional **tycho-surefire:test** goal) as following:

- create a new test that matches the pattern `*IT.java` or `PluginTest*.java` (or configure a different pattern that does not intersects with the surefire test pattern)
- run the build with `mvn verify`

Summarizing

-  **tycho-surefire:test** works as before: it is automatically activated in projects with packaging type `eclipse-test-plugin` (which are meant to contain only tests), it runs in the phase `integration-test` and makes the build fail if a test fails.
-  **tycho-surefire:integration-test** is meant to be used in projects with packaging type `eclipse-plugin` (which are meant to contain both production code and tests, in a separate source folder) and it is meant to be bound to the phase `integration-test`, but following the `maven-failsafe` paradigm: if a test fails, the build does not fail in the phase `integration-test`, but in the phase `verify`.

:warning: If you where previously using `-Dtest=....` on the root level of your build tree it might now be necessary to also include `-Dsurefire.failIfNoSpecifiedTests=false` as maven-surefire might otherwise complain about

> No tests were executed! (Set -DfailIfNoTests=false to ignore this error.)

for your eclipse-plugin packaged project if they do not match anything (the error message is a bit misleading, this is tracked in [SUREFIRE-1910](https://issues.apache.org/jira/browse/SUREFIRE-1910)).


### [Enhanced support for debug output in surefire-tests](https://github.com/eclipse/tycho/issues/52)
tycho-surefire now support to set .options files for debugging through the new debugOptions parameter, example:

```xml
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

### [Support for site/repository-reference/@location in eclipse-repository](https://github.com/eclipse/tycho/issues/141)
Tycho now correctly supports repository references in `category.xml`: in the previous versions, the `content` XML generated by Tycho only contained a reference to the metadata referred repository, but not to the artifacts referred repository, resulting in failures in installations (the verification phase went fine, but the actual installation could not be performed by p2).

### Add PGP signatures to artifacts in p2 repositories

A new mojo [tycho-gpg-plugin:sign-p2-artifacts-mojo](https://www.eclipse.org/tycho/sitedocs/tycho-gpg-plugin/sign-p2-artifacts-mojo.html) was added to add GPG signatures to artifacts metadata. Those GPG signatures are later expected to be used by p2 during installation to verify integrity and build trust in installed components.

### [Support for "additional.bundles" directive in build.properties](https://bugs.eclipse.org/bugs/show_bug.cgi?id=572481)

Tycho now understands the `additional.bundles` directive in the `build.properties` file and properly resolves them during dependency resolution.

### Create p2 repository referencing Maven artifacts

A new mojo [tycho-p2-repository-plugin:assemble-maven-repository](https://www.eclipse.org/tycho/sitedocs/tycho-p2/tycho-p2-repository-plugin/assemble-maven-repository-mojo.html) was added to enable creation of p2 repositories directly from Maven artifact references. This removes the usual need to create a target definition and a category.xml for this task.

### [Skip Tycho dependency-resolution for clean-only builds by default](https://github.com/eclipse/tycho/issues/166)

To speed up Maven `clean`-only builds, Tycho's dependency resolution is now skipped, if the only phase specified is one from the clean lifecycle, namely `clean`,`pre-clean`,`post-clean`.
Previously one had to specify the property `-Dtycho.mode=maven` to skip dependency resolution.

### [Automatically translate maven-license information to OSGi `Bundle-License` header](https://github.com/eclipse/tycho/issues/177)

If your pom contains license information, Tycho automatically creates (if not already present) OSGi `Bundle-License` header for you. This behavior can be controlled with [deriveHeaderFromProject](https://www.eclipse.org/tycho/sitedocs/tycho-packaging-plugin/package-plugin-mojo.html#deriveHeaderFromProject) setting.

## 2.3.0

### Official Equinox Resolver used for dependency resolution (stricter and can produce errors for split packages)

[Bug 570189](https://bugs.eclipse.org/bugs/show_bug.cgi?id=570189) Tycho now uses the same resolver as Equinox uses at runtime. This resolver is stricter and more correct than the previous one, and as a result should provide resolution results that are much more consistent with actual dependency resolution at runtime.

However, this change makes [bug 403196](https://bugs.eclipse.org/bugs/show_bug.cgi?id=403196) more visible in some cases with split package, for example
<pre>
  Unresolved requirement: Require-Bundle: org.eclipse.equinox.security
    -> Bundle-SymbolicName: org.eclipse.equinox.security; bundle-version="1.3.600.v20210126-1005"; singleton:="true"
       org.eclipse.equinox.security [1]
         Unresolved requirement: Import-Package: org.eclipse.core.runtime; registry="split"
</pre>
which means that the p2 resolution succeeds while it actually failed at providing the <tt>Import-Package: org.eclipse.core.runtime; registry="split"</tt> and later OSGi resolution will find it's missing and complain.<br>
In such case, the workaround/solution is to ensure that the bundle that provides <tt>Import-Package: org.eclipse.core.runtime; registry="split"</tt> gets added as part of p2 resolution despite [bug 403196](https://bugs.eclipse.org/bugs/show_bug.cgi?id=403196), typically by adding <tt>org.eclipse.equinox.registry</tt> to the <tt>Required-Bundle</tt> of the bundles being built, or by adding it to <tt>target-platform-configuration</tt> as described in https://www.eclipse.org/tycho/sitedocs/target-platform-configuration/target-platform-configuration-mojo.html#dependency-resolution .

### Enable reuse of workspace by tycho-eclipserun-plugin

[Bug 570477](https://bugs.eclipse.org/bugs/show_bug.cgi?id=570477), The <tt>tycho-eclipserun-plugin</tt> now has a configuration-parameter named <tt>clearWorkspaceBeforeLaunch</tt> to specify if the workspace should be cleared before running eclipse or not (default is <tt>true</tt>, matching the behavior until now). If the value is <tt>false</tt>, the workspace of the previous run is reused (if present), if the value is <tt>true</tt> the workspace-directory (i.e. the 'data' directory within the <tt>work</tt>-directory) and its content is deleted.

### A mojo to "fix" modified metadata in artifact repository (artifacts modified after after aggregation)

The [https://ci.eclipse.org/tycho/job/tycho-sitedocs/lastSuccessfulBuild/artifact/target/staging/tycho-p2/tycho-p2-repository-plugin/fix-artifacts-metadata-mojo.html org.eclipse.tycho:tycho-p2-repository-plugin:fix-artifacts-metadata] was added. It updates the artifact repository metadata checksums and size of modified artifacts in the given folder. This can be used if some other mojo (e.g. jar-signer) modifies the repository artifacts after the assemble-repository step. An example could be found in the [https://github.com/eclipse/tycho/tree/master/tycho-its/projects/jar-signing-extra jar-signing-extra] integration test

### A mojo to remap Maven artifacts to Maven URLs in artifact repository

The [https://ci.eclipse.org/tycho/job/tycho-sitedocs/lastSuccessfulBuild/artifact/target/staging/tycho-p2/tycho-p2-repository-plugin/remap-artifacts-to-m2-repo-mojo.html org.eclipse.tycho:tycho-p2-repository-plugin:remap-artifacts-to-m2-repo] was added. It modifies the artifact metadata of the provided p2 repository by adding extra mapping rules for artifacts the can be resolved to Maven repositories so the URL under Maven repository is used for fetching and artifact is not duplicated inside this repo. See [https://git.eclipse.org/c/tycho/org.eclipse.tycho.git/tree/tycho-its/projects/p2Repository.mavenRepo/pom.xml#n28 this example].

### Target files can be specified directly now

[Bug 571520](https://bugs.eclipse.org/bugs/show_bug.cgi?id=571520) allow to specify a target file as an alternative to a target artifact
```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>target-platform-configuration</artifactId>
    <version>${tycho-version}</version>
    <configuration>
        <target>
            <file>jetty.target</file>
        </target>
    </configuration>
</plugin>
```
### Multi-platform product packaging

[Bug 572082](https://bugs.eclipse.org/bugs/show_bug.cgi?id=572082), allow applications to use bundle pools in order to have a "cross platform" installation structure without duplicating the bundles. To activate the multi-platform package, simply add <tt><multiPlatformPackage>true</multiPlatformPackage></tt> to the product definition in the <tt>pom.xml</tt> file.


## 2.2.0

## Support for m2e PDE Maven target locations

[Bug 568729](https://bugs.eclipse.org/bugs/show_bug.cgi?id=568729), [bug 569481](https://bugs.eclipse.org/bugs/show_bug.cgi?id=569481) m2e includes a new feature that allows the usage of regular maven artifacts to be used in PDE target platforms. Support for this new location type was also added to tycho, you could read more about this new feature in the following [article](https://xn--lubisoft-0za.gmbh/en/articles/using-maven-artifacts-in-pde-rcp-and-tycho-builds/).

#### Allow parallel dependency resolving

[Bug 568446](https://bugs.eclipse.org/bugs/show_bug.cgi?id=568446) When using [parallel builds](https://cwiki.apache.org/confluence/display/MAVEN/Parallel+builds+in+Maven+3) the initial dependency resolution is now also executed in parallel.

### Delay download of p2 artifacts for non-plugin modules

[Bug 567760](https://bugs.eclipse.org/bugs/show_bug.cgi?id=567760) Tycho now stops downloading p2 artifacts from remote sources too early and relies mostly on p2 metadata to compute target platform and build order without downloading the artifacts. Downloads are then instead delayed to when the artifact files are really necessary (to compute compilation build path, to generate a p2 repository...). As a result performance will be improved as less artifacts may be downloaded, and some steps like `mvn clean` or `mvn validate` can avoid downloading artifacts to process, and the most probable build failures will be reported faster.

Note that this is specifically visible for <tt>eclipse-feature</tt> and <tt>eclipse-repository</tt> modules; other module types like <tt>eclipse-plugin</tt> or <tt>eclipse-test-plugin</tt> do still require to download artifacts early to create the build path. Further improvements remain possible on that topic.

### Tycho Source Feature Generation moved from tycho extras to tycho core

[Bug 568359](https://bugs.eclipse.org/bugs/show_bug.cgi?id=568359) Historically the tycho-source-feature-plugin was located in tycho extras but could be confusing because they often are used in conjunction with to each other because a source-bundle is hardly useful without corresponding source-feature. With the merge of tycho-core and tycho-extras these separation becomes even more obsolete.

From now on, the tycho-source-plugin also includes the tycho-source-feature-plugin, the old one is deprecated and will be removed in the next major release.

Migration is rather simple, just add a new execution to the tycho-source-plugin
```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-source-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
        <execution>
        <id>plugin-source</id>
        <goals>
            <goal>plugin-source</goal>
        </goals>
        </execution>
         <execution>
        <id>feature-source</id>
        <goals>
            <goal>feature-source</goal>
        </goals>
        <configuration>
        <!-- put your configuration here -->
        </configuration>
        </execution>
    </executions>
</plugin>
```

Beside this, the new mojo does support one additional configuration option 'missingSourcesAction' that can have one of the following two values:
* FAIL: this is like the old mojo works, any missing source bundle that is not excluded will fail the build
* WARN: this is the default, a warning will be omitted listing all missing plugins/features that are not excluded

### Support for consuming maven artifacts made of zipped P2 update sites ===

[Bug 398238](https://bugs.eclipse.org/bugs/show_bug.cgi?id=398238) Tycho now supports in target files and in <repository> elements URLs of the form mvn:groupId:artifactId:version[:packaging[:classifier]] to be used for a repository.

For example
```xml
<repository>
    <id>activiti</id>
    <layout>p2</layout>
    <url>mvn:org.activiti.designer:org.activiti.designer.updatesite:5.11.1:zip</url>
</repository>
```

### Support for excluding files in build.properties ===

[Bug 568623](https://bugs.eclipse.org/bugs/show_bug.cgi?id=568623) Tycho now supports in build properties files to exclude files in library.

For example
```
output.mycodelib.jar = bin/
bin.includes = META-INF/,\
               mycodelib.jar

source.mycodelib.jar = src/
exclude.mycodelib.jar = **/*.txt
```

