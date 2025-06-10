# Eclipse Tycho 5: Release notes

This page describes the noteworthy improvements provided by each release of Eclipse Tycho.
If you are reading this in the browser, then you can quickly jump to specific versions by using the rightmost button above the headline:
![grafik](https://github.com/eclipse-tycho/tycho/assets/406876/7025e8cb-0cdb-4211-8239-fc01867923af)

## 5.0.0

## Java 21 required to run Tycho build

Tycho now requires to run with Java 21. As before it can still compile / test for older java releases.

## Support for JVMs < 1.8 dropped

Previously Tycho could detect JVMs down to Java 1.1 what requires running some java code to run on these platforms.
As it becomes harder over time to actually compile code for such old targets while compilers are dropping support,
Tycho from now on by default only supports to detect JVMs with version 1.8 or higher.

Users who absolutely need this can configure a previous version of the `tycho-lib-detector` with the system property `tycho.libdetector.version`

## TestNG support improved / TestNG deprecated

The previous Tycho TestNG support was rather flawed it worked but required some hacks, this is now improved so one can consume
directly official TestNG artifacts.

This also revealed that TestNG itself has some major flaws and only works in an old `6.9.10` version:

- [TestNG should have a DynamicImport-Package](https://github.com/testng-team/testng/issues/3210)
- [TestNG is no longer working in OSGi environments](https://github.com/testng-team/testng/issues/1678)
- [META-INF/MANIFEST.MF not correctly generated](https://github.com/testng-team/testng/issues/1190)
- [Support to setup a method selector instance directly](https://github.com/testng-team/testng/issues/3211)

**Because of this TestNG is deprecated** and will be removed in a future version unless someone express interest in TestNG and helps improving
it so we can upgrade to later versions.

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
## new `quickfix`, `cleanup` and `manifest` mojo

Keeping code up-todate is a daunting task and Eclipse IDE can be a great help due to its offering for automatic quick-fixes and cleanup actions.
Still this has usually be applied manually (or automatic on save) and requires some user interaction.

There is now a new `tycho-cleancode:cleanup`, `tycho-cleancode:quickfix` and `tycho-cleancode:manifest` mojo that help with these things to be called via automation or part of the build, 
the mojos run by default in the process-sources phase so any later compilation can verify the outcome easily. Due to the way they work internally,
they require an eclipse project to be present and configured currently.

The `tycho-cleancode:cleanup` mojo allows to configure the desired cleanup profile in the pom (maybe inside a profile), the values are those that you will find 
when exporting a profile from the IDE. If no profile is given the default from the project are used:

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-cleancode-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
      <execution>
        <id>cleanup</id>
        <goals>
          <goal>cleanup</goal> 
        </goals>
        <configuration>
            <cleanUpProfile>
                <cleanup.static_inner_class>true</cleanup.static_inner_class>
            </cleanUpProfile>
        </configuration>
      </execution>
    </executions>
</plugin>
```

The `tycho-cleancode:quickfix` mojo simply apply the quick fix with the highest relevance for resolution and can be enabled like this (maybe inside a profile):

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-cleancode-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
      <execution>
        <id>quickfix</id>
        <goals>
          <goal>quickfix</goal> 
        </goals>
      </execution>
    </executions>
</plugin>
```

The `tycho-cleancode:manifest` mojo apply the 'Organize Manifest' action from PDE that allows to automatically cleanup manifest headers, 
that are usually hard to maintain manually and can be enabled like this (maybe inside a profile):

```xml
<plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-cleancode-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
      <execution>
        <id>manifest</id>
        <goals>
          <goal>manifest</goal> 
        </goals>
        <configuration>
            <calculateUses>true</calculateUses>
        </configuration>
      </execution>
    </executions>
</plugin>
```

## new `check-dependencies` mojo

When using version ranges there is a certain risk that one actually uses some methods from never release and it goes unnoticed.

There is now a new `tycho-baseline:dependencies` mojo that analyze the compiled class files for used method references and compares them to
the individual artifacts that match the version range. To find these versions it uses the maven metadata stored in P2 as well as
the eclipse-repository index to find possible candidates.

If any problems are found, these are written by default to `target/versionProblems.txt` but one can also enable to update the version ranges
according to the discovered problems, a configuration for this might look like this:

```xml
   <plugin>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-baseline-plugin</artifactId>
    <version>${tycho-version}</version>
    <executions>
      <execution>
        <id>checkit</id>
        <goals>
          <goal>check-dependencies</goal>
        </goals>
        <configuration>
        	<applySuggestions>true</applySuggestions>
        </configuration>
      </execution>
    </executions>
  </plugin
```

Because this can be a time consuming task to fetch all matching versions it is best placed inside a profile that is enabled on demand.

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

### Generation of XML report in tycho-apitools-plugin

`tycho-apitools-plugin:verify` now creates an XML report of API Tools warnings compatible with
Jenkins *warnings-ng* native format. The default location `target/apianalysis/report.xml` can
be overridden using `<report>` or the `tycho.apitools.report` property.

The logs can then be parsed on Jenkins using

```groovy
post {
    always {
        recordIssues enabledForFailure: true, tools: [issues(id: 'apichecks', name: 'API', pattern: '**/target/apianalysis/report.xml')]
    }
}
```

The previous way of enhancing ECJ compiler logs with the API Tools warnings via `<enhanceLogs>`
/ `<logDirectory>` has been removed.


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

### Support for JUnit prior 4.7 removed

Tycho for a long time has shipped with support for older JUnit 3/4 versions.
As JUnit 3/4 is actually EOL we no longer support version before 4.7 anymore.

For users that are previously have used a specific provider in their configuration (like `junit47`) this means they need
to change that to use only `junit4` from now on.

### Support for JUnit 5 prior 5.9 removed

JUnit 5.9 (included in Eclipse 2022-12) is the lowest version we support as this was where platform switched using th original
junit artifacts and previous versions from orbit has several compatibility issues.

### Only one JUnit 5 provider

Historically Tycho has multiple providers for JUnit 5 to support different version. As Tycho is loading providers from
the maven classpath users that really require an older version can always choose to stay on a previous release (of Tycho)
for that part like it is already possible with JDT compiler.

Because of this Tycho will now only ship with one JUnit 5 provider that is updated alongside with the JUnit 5 release
like we already do for other dependencies like Eclipse Platform.

For users that are previously have used a specific provider in their configuration (like `junit59`) this means they need
to change that to use only `junit5` from now on.

## 4.x

For release notes of the Tycho 4 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-4.0.x/RELEASE_NOTES.md)

## 3.x

For release notes of the Tycho 3 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-3.0.x/RELEASE_NOTES.md)

## 2.x

For release notes of the Tycho 2 see [here](https://github.com/eclipse-tycho/tycho/blob/tycho-2.7.x/RELEASE_NOTES.md)