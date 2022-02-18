# Eclipse Tycho: Release notes

This page describes the noteworthy improvements provided by each release of Eclipse Tycho.

## 3.0.0 (under development)

### Migration guide 2.x -> 3.x

#### Pack200

Pack200 technology is obsolete and no longer supported after Java 13. Tycho removed all support for dealing with pack.gz files including pack200 specific plugins, various options in Mojos and support for resolving fetching in core. 

Omit any pack200 specific plugins, options and in any other way dealing with ***.pack.gz** files.
The following are removed:
- Plugins
	- tycho-pack200a-plugin
	- tycho-pack200b-plugin
- Mojo options
	- TargetPlatformConfigurationMojo' includePackedArtifacts
	- MirrorMojo's includePacked
	- PublishFeaturesAndBundlesMojo reusePack200Files
	
- ...


### New Feature ...

## 2.7.0

### Tycho-Pomless will become a tycho-core extension

Tycho pomless has started as a small experiment in tycho-extras. Over time it has grown to a fully-fledged solution to build pde-based artifacts with less effort and nearly zero additional configuration.

Neverless, the name "pomless" was always a bit misleading, as actually we have reduced the number required poms to one 'main-pom' it is still not pomless and actually allows poms to be used where suitable.
Because of this, an to not limit the usage to "pomless" with this version a new core-extension is available name 'tycho-build', that effectively does what tycho-extras-pomless does but in the context of 'core' and is open to further improvements 
(maybe some time offering an option to not needing a pom at all).

All that needs to be done is to replace the old 
```
<extension>
	<groupId>org.eclipse.tycho.extras</groupId>
	<artifactId>tycho-pomless</artifactId>
	<version>2.7.0</version>
</extension>
```

with

```
<extension>
	<groupId>org.eclipse.tycho</groupId>
	<artifactId>tycho-build</artifactId>
	<version>2.7.0</version>
</extension>
```

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

## 2.6.0

### Delayed classpath computation
Previously the classpath of a project was computed in the maven-setup phase, this [has several restrictions](https://github.com/eclipse/tycho/issues/460).
Tycho now delays the classpath computation to a later stage (`initialize` phase).

If you want to perform the classpath validation in the `validate` phase of your build you can force classpath computation with the following snippet:

```
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

```
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

```
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
It is now possible to resolve the JDT 'JUnit Classpath Container', for this do the following:

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
```
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
```
<source lang="xml">
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
</source>
```

Beside this, the new mojo does support one additional configuration option 'missingSourcesAction' that can have one of the following two values:
* FAIL: this is like the old mojo works, any missing source bundle that is not excluded will fail the build
* WARN: this is the default, a warning will be omitted listing all missing plugins/features that are not excluded

### Support for consuming maven artifacts made of zipped P2 update sites ===

[Bug 398238](https://bugs.eclipse.org/bugs/show_bug.cgi?id=398238) Tycho now supports in target files and in <repository> elements URLs of the form mvn:groupId:artifactId:version[:packaging[:classifier]] to be used for a repository.

For example
```
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

