# Contributing to Eclipse Tycho

Thanks for your interest in this project.

## Table of Contents
1. [Try SNAPSHOTs and report issues](#try-snapshots-and-report-issues)
2. [Development Environment](#development-environment)
    1. [Prerequisites](#prerequisites)
    2. [Using the Eclipse Installer (Oomph)](#using-the-eclipse-installer-oomph)
    3. [Manual Setup](#manual-setup)
3. [Tests](#tests)
    1. [Tycho Integration Tests](#tycho-integration-tests)
    2. [Writing Tycho Integration Tests](#writing-tycho-integration-tests)
        1. [Tips on the naming of integration tests](#tips-on-the-naming-of-integration-tests)
4. [🏗️ Build & Test](#🏗️-build--test)
5. [Debugging](#debugging)
6. [Commits](#commits)
    1. [Message Guidelines](#message-guidelines)
    2. [Granularity](#granularity)
7. [Submit Patch](#submit-patch)
8. [Increasing Versions](#increasing-versions)
9. [Backporting](#backporting)
10. [Advanced development tricks](#advanced-development-tricks)
    1. [Building Tycho against a locally built version of p2](#building-tycho-against-a-locally-built-version-of-p2)
    2. [Running with a locally built version of the JDT compiler](#running-with-a-locally-built-version-of-the-jdt-compiler)
    3. [Updating the Equinox and JDT dependencies of Tycho](#updating-the-equinox-and-jdt-dependencies-of-tycho)
    4. [Profiling the Tycho build](#profiling-the-tycho-build)
        1. [Add timestamps to Maven logging](#add-timestamps-to-maven-logging)
        2. [Add Maven profile](#add-maven-profiler)
        3. [Yourkit YouMonitor](#yourkit-youmonitor)
11. [Contact](#contact)
12. [👔 Process and Legal](#👔-process-and-legal)
    1. [Eclipse Development Process](#eclipse-development-process)
    2. [Eclipse Contributor Agreement](#eclipse-contributor-agreement)

## Try SNAPSHOTs and report issues

To enable SNAPSHOTs, make sure the following Maven plugin-repository is available to your build: https://repo.eclipse.org/content/repositories/tycho-snapshots/.
This can be accomplished by adding the following snippet to your (parent) pom.xml or settings.xml:
```xml
<pluginRepositories>
    <pluginRepository>
        <id>tycho-snapshots</id>
        <url>https://repo.eclipse.org/content/repositories/tycho-snapshots/</url>
    </pluginRepository>
</pluginRepositories>
```
Make sure you have set the property for the Tycho version (e.g. `tycho-version`) to `<version-under-development>-SNAPSHOT` in the project being built.

For documentation of the most recent snapshot build, see the [Snapshot Tycho Site](https://tycho.eclipseprojects.io/doc/master/index.html).

If you identify an issue, please try to reduce the case to the minimal project and steps to reproduce, and then report the bug with details to reproduce
and the minimal reproducer project to Tycho's [issue tracker](https://github.com/eclipse-tycho/tycho/issues).

## Development environment

[![Create Eclipse Development Environment for Tycho](https://download.eclipse.org/oomph/www/setups/svg/tycho.svg)](https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-tycho/tycho/master/setup/TychoDevelopmentConfiguration.setup&show=true "Click to open Eclipse-Installer Auto Launch or drag into your running installer")

&nbsp;&nbsp;&nbsp;or just&nbsp;&nbsp;&nbsp;

<a href="https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/redirect.html"><img src="https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/cloneToEclipseBadge.png" alt="Clone to Eclipse IDE"/></a>

### Prerequisites

Java 17 and Maven 3.9.0, or newer.

If your Internet connection uses a proxy, make sure that you have the proxy configured in your [Maven settings.xml](https://maven.apache.org/settings.html).

### Using the Eclipse Installer (Oomph)

Step-by-step instructions:

1. Download the [Eclipse Installer](https://wiki.eclipse.org/Eclipse_Installer).
2. Start the installer using the `eclipse-inst` executable.
3. On the first page (product selection), click the preference button in the top-right corner and select the _Advanced Mode_ .
4. If you are behind a proxy, at this point you might want to double-check your network settings by clicking on the _Network Proxy Settings_ at the bottom.
5. Select _Eclipse IDE for Eclipse Committers_ . Click _Next_ .
6. Under _Eclipse.org_ , double-click on _Tycho_ (single click is not enough!). Make sure that _Tycho_ is shown in the table at the bottom. Click _Next_.
7. You can edit the _Installation Folder_ , but you do not have to select the _Target Platform_ here, this will be set later automatically. By choosing _Show all variables_ at the bottom of the page, you are able to change other values as well but you do not have to. (Unless you have write access to the GitHub repository, make sure you select "HTTPS read-only" in the dropdown "Tycho Github repository"). Click _Next_.
8. Press _Finished_ on the _Confirmation_ page will start the installation process.
9. The installer will download the selected Eclipse version, starts Eclipse and will perform all the additional steps (cloning the git repos, etc...). When the downloaded Eclipse started, the progress bar in the status bar shows the progress of the overall setup.
10. Once the _Executing startup task_ job is finished you should have all the Tycho and Tycho Extras projects imported into 2 working sets called _Tycho_ and _Tycho Extras_ .
11. Some Projects might still have errors. Select them (or all) and choose _Maven > Update Project.._ from the context menu. De-select _Clean projects_ in the shown dialog and press _OK_ to update the projects. After that, no more errors should be present.

### Manual Setup

The preferred and easier way is to follow the instructions above, but you could also setup your environment manually:

1. Get an [Eclipse IDE](https://www.eclipse.org/downloads/eclipse-packages/) with a recent version of the [Maven integration for Eclipse (m2eclipse)](https://www.eclipse.org/m2e/) and Eclipse PDE installed. m2eclipse is included in various Eclipse packages, e.g. the _Eclipse IDE for Eclipse Committers_ package. To add m2eclipse to your existing Eclipse installation, install it from the Eclipse Marketplace.
2. Clone this repository (via CLI or EGit)
3. In Eclipse, use `File > Import > Existing Maven Projects``, select the root directory of the sources, and import all projects. If prompted by m2eclipse, install the proposed project configurators and restart Eclipse.
4. For Tycho only: Configure the target platform: Open the file `tycho-bundles-target/tycho-bundles-target.target` and click on _Set as Target Platform_ in the upper right corner of the target definition editor.


The result should be an Eclipse workspace without build errors. M2eclipse may take some time to download the required libraries from Maven central.

* If there are compile errors in the projects `org.eclipse.tycho.surefire.junit`, `org.eclipse.tycho.surefire.junit4`,  `org.eclipse.tycho.surefire.junit47`, or `org.eclipse.tycho.surefire.osgibooter`, just select these projects and manually trigger an update via _Maven > Update project..._ from the context menu.

## Tests

Tycho has two types of tests: unit tests (locally in each module) and a global integration test suite in module tycho-its.

Unit tests are preferred if possible because they are in general much faster and better targeted at the functionality under test. Integration tests generally invoke a forked Maven build on a sample project (stored under projects/) and then do some assertions on the build results.

### Tycho Integration Tests

The Tycho integration tests are located in the project `tycho-its`. To run all Tycho integration tests, execute
```
$ mvn clean install -f tycho-its/pom.xml`
``` 
To run a single integration test, select the test class in Eclipse and run it as "JUnit Test", or run
```
$ mvn clean verify -f tycho-its/pom.xml -Dtest=MyTestClass
``` 
from the command line, replacing `MyTestClass` with the test class to run (without `.java`).

_Background information on the Tycho integration tests_

The integration tests trigger sample builds that use Tycho. These builds expect that Tycho has been installed in the local Maven repository. This is why you need to build Tycho through a `mvn install` before you can run the integration tests.

Alternatively, e.g. if you are only interested in modifying an integration test and do not want to patch Tycho itself, you can configure the integration tests to download the current Tycho snapshot produced by the [Tycho CI builds](https://hudson.eclipse.org/tycho/view/CI). To do this, you need to edit the Maven settings stored in `tycho-its/settings.xml` and add the tycho-snapshots repository as described in [[Getting Tycho]]. (Advanced note: The integration tests can also be pointed to a different settings.xml with the system property `tycho.testSettings`.)

### Writing Tycho Integration Tests

The tycho integration tests are located in the [tycho-its](https://github.com/eclipse-tycho/tycho/tree/master/tycho-its) subfolder of the repository. Creating a new integration test usually includes the following steps:

1. Create a new folder in the [projects](https://github.com/eclipse-tycho/tycho/tree/master/tycho-its/projects) directory (see below for a good naming, but this could be improved as part of the review so don't worry about choosing an intermediate name first). Usually, you want to use `${tycho-version}` as a placeholder in your pom so the execution picks up the current tycho version
2. Check if there is already a suitable test class available or simply create your own in the [test](https://github.com/eclipse-tycho/tycho/tree/master/tycho-its/src/test/java/org/eclipse/tycho/test/) directory (again the name could be improved later on if required). The usual pattern for a self-contained test case that fails the build is:
```java
@Test
public void test() throws Exception {
    Verifier verifier = getVerifier("your-project-folder-name", false);
    verifier.executeGoals(asList("verify"));
    verifier.verifyErrorFreeLog();
}
```
3. You might want to check for additional constraints. See the [Verifier](https://maven.apache.org/shared/maven-verifier/apidocs/index.html) for available options.
4. If you don't want to run the full integration build you can simply go to the project directory and run

```
$ mvn clean verify -Dtycho-version=<version of tycho where you see the issue>
```
to see the outcome of your created test.


#### Tips on the naming of integration tests

The hardest part of writing Tycho integration tests is the naming. While names are mostly important for readability, there were also cases where the ID "feature" was used multiple times, and hence a test used the build result of a different integration test.

Therefore, here are a few tips for writing good integration tests:
* Test project name: Although many existing tests have a bug number in the name, this is **not** the recommended naming scheme. Since integration tests can take some time to execute, it may be a good idea to test related things in one test. <br>So name the test projects in a way that they can be found, and that related tests are sorted next to each other, e.g. in the form <tt>&lt;component&gt;.&lt;aspect&gt;</tt>.
* Package: Should be <tt>org.eclipse.tycho.test.&lt;component&gt;</tt> (without the aspect so that we don't get an excessive number of packages)
* Test project groupIds: Should be <tt>tycho-its-project.&lt;component&gt;.&lt;aspect&gt;</tt> plus a segment for the reactor in case of multi-reactor tests. The groupId is particularly important if the test project is installed in the local Maven repository. Avoid `install`, use `verify` if possible.
* Test project artifact ids have to be the same as the ID of the feature/bundle and need a unique prefix, e.g. the first letters of each segment of the project name.

## 🏗️ Build & Test

From the root directory of your local Tycho git-repository clone run the following Maven commands...
* to check if the compilation and all tests succeed:
    * `mvn clean verify -Pits`
* to install your version of Tycho into your local Maven repository (skips all tests for faster installation):
    * `mvn clean install -DskipTests`

In order to test your changes of Tycho locally in a project-build, install your modified Tycho locally as described above
and use the corresponding Tycho (probably snapshot) version in the project being build.
You can also debug that build with the steps below (from here you can jump to step 3 immediately).

## Debugging

In order to debug Tycho plugins inside Eclipse:

1. Get the Tycho sources in Eclipse.
2. Create/get a project that highlights the bug.

Inside the Eclipse IDE:

3. Create a Maven Run-Configuration in your Tycho Eclipse workspace to build the project and specify goals, profiles and properties as required.
4. Launch the Maven configuration from your Eclipse in Debug mode.

Or on the command-line interface:

3. Run the project-build using `mvnDebug` (instead of `mvn`) and specify goals, profiles and properties as required.
4. Go into your Eclipse, use `Debug > Remote Java Application` and select `port 8000` to attach the Eclipse Debugger.

Before debugging a build, make sure that your local Tycho sources correspond to the Tycho version used by the project being build.
Otherwise, the debugger might show unexpected behavior.

## Commits

### Message Guidelines

Start with `Bug: <number>` stating the bug number the change is related to; this will enable the Eclipse genie bot to automatically cross-link bug and pull request.

Also in the first line, provide a clear and concise description of the change.

Add one blank line, followed by more details about the change. This could include a motivation for the change and/or reasons why things were done in the particular way they are done in the change.

### Granularity

Make small commits, yet self-contained commits. This makes them easy to review.

Do not mix concerns in commits: have a commit do a single thing. This makes them reviewable 'in isolation'. This is particularly important if you need to do refactorings to the existing code: Refactorings tend to lead to large diffs which are difficult to review. Therefore make sure to have separate commits for refactorings and for functional changes.

## Submit patch

As a GitHub pull request. Create a branch off of `master` with a helpful name, like `issue_<issue number>_reproducer` if you are providing an integration test for an existing issue, or `compiler-plugin-bug` if you are fixing a bug with the compiler plugin.

Create a branch off of master even for small bug fixes. Changes from `master` can be backported automatically to older versions, and it's important that master not miss any fixes that older versions have. See [Backporting](#backporting) for more information.

## Increasing versions

The micro version will only be used for critical bug-fix releases, in most other cases we will have increased the current minor version already so nothing has to be done.

The following list contains changes that only can happen between major version updates:

- changing the Java version to run the build
- requiring a new minimum maven version (e.g. once we require maven 4.x)
- requiring to change their pom.xml in a non-trivial way (e.g. besides
changing some configuration value in an existing mojo, or providing a drop-in replacement in the migration guide)

If you require such a change, please note that in the issue and we will assign the next major release to it. Those changes would not be merged until the next major release. Keep your changes small and local as it possibly takes some time and you probably have to catch up with minor changes in the meantime.

## Backporting

In general, we do not backport fixes but recommend using the current [tycho snapshot](https://github.com/eclipse/tycho/wiki#getting-tycho-snapshots) builds to help move things forward and have safe releases.

Still backporting is possible with mainly two options:

1. You prepare the necessary things  with PR so they can be reviewed and merged
2. You pay someone to perform the required steps and drive the release, see https://github.com/eclipse-tycho/tycho#getting-support for details.

If you choose one of the first options, backporting usually includes the following steps:

1. Check out the branch you are interested in, they are always named `tycho-<major>.<minor>.x`.
2. Make sure the branch is at the next version, e.g. the last release was `3.0.0` the next version should be `3.0.1-SNAPSHOT`, if not use the following command to update the version and create a PR with the changed files: `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<NEXT_VERSION>-SNAPSHOT`.
3. Backport the fix to the branch and add a hint to the RELEASE_NOTES.md of that branch that describes what was backported and create a PR targeting your branch of interest so it could be verified, reviewed an merged.
4. Once it is merged and the SNAPSHOT is available, test your fix
5. Look through the issues that were fixed after your target release and identify more items that seem useful and repeat with step 3.
6. Once there is a noticeable amount of things backported that could justify a release create an issue asking for a bugfix release to be performed.

## Advanced development tricks

### Building Tycho against a locally built version of p2

Tycho makes heavy use of p2 functionality. Therefore it may be useful to try out patches of p2 without waiting for a new p2 release, or even just the next nightly build. With the following steps, it is possible to build Tycho against a locally built version of p2.

1. Get the p2 sources (see [p2 project information](https://projects.eclipse.org/projects/rt.equinox.p2/developer))
2. Make changes in the p2 sources, **(!) don't forget to increase the version of that bundle otherwise your changes will be overwritten with the current release version (!)**
3. Build the changed p2 bundles individually with <tt>mvn clean install -Pbuild-individual-bundles</tt> (see [Equinox/p2/Build](https://wiki.eclipse.org/Equinox/p2/Build) for more information)
4. Build at least the Tycho module tycho-bundles-external with <tt>mvn clean install</tt> - you should see a warning that the locally built p2 bundles have been used.
Then the locally built Tycho SNAPSHOT includes the patched p2 version.

Note: Tycho always allows references to locally built artifacts, even if they are not part of the target platform. Therefore you may want to clear the list of locally built artifacts (in the local Maven repository in `.meta/p2-local-metadata.properties`) after you have finished your trials with the patched p2 version.

### Running with a locally built version of the JDT compiler

Tycho internally calls the Eclipse Java Compiler, therefore it might be useful to try your patches to ECJ without waiting for a new release, or even just the next nightly build. With the following steps, it is possible to run a Tycho build with a locally built version of ECJ;

1. Get the sources from https://github.com/eclipse-jdt/eclipse.jdt.core
2. Make changes in the ECJ sources, **(!) don't forget to increase the version of that bundle otherwise your changes will be overwritten with the current release version (!)**
3. Build the `eclipse.jdt.core/org.eclipse.jdt.core` module with `mvn clean package -Pbuild-individual-bundles -Dtycho.localArtifacts=ignore -DskipTests`
4. Install the result in your local maven repository under a new version `mvn install:install-file -Dfile=<path to>/eclipse.jdt.core/org.eclipse.jdt.core/target/org.eclipse.jdt.core-<version>-batch-compiler.jar -DgroupId=org.eclipse.jdt -DartifactId=ecj -Dversion=<yournewversion> -Dpackaging=jar`
5. Now edit the `pom.xml` of the project you'd like to test and either edit or insert
```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-compiler-plugin</artifactId>
  <version>${tycho-version}</version>
  <dependencies>
    <dependency>
      <groupId>org.eclipse.jdt</groupId>
      <artifactId>ecj</artifactId>
      <version><yournewversion></version>
    </dependency>
  </dependencies>
</plugin>
```

### Updating the Equinox and JDT dependencies of Tycho

Tycho has Maven dependencies to Equinox and JDT, so these artifacts are used from the Maven Central repository.

### Profiling the Tycho build

To understand where the build spends most of its time, you can try the following approaches:

#### Add timestamps to Maven logging

You can [add a timestamp to each log line](https://blogs.itemis.com/en/in-a-nutshell-adding-timestamps-to-maven-log-output) produced by Maven. This is the easiest method, but you have to do the calculation of the runtime of different goals yourself.

#### Add Maven profiler

Download the [Maven profiler extension](https://github.com/jcgay/maven-profiler) and add it to your aggregator project. It will produce an HTML report for each goal.

To install it, simply add an [extensions.xml file to your project aggregator](https://github.com/jcgay/maven-profiler#maven--33x) with the Maven coordinates of the profiler. That way Maven will automatically download the profiler during the build.

To use the profiler, [set the system property](https://github.com/jcgay/maven-profiler#usage).

#### Yourkit YouMonitor

[Yourkit YouMonitor](https://www.yourkit.com/youmonitor/) (not to be confused with Yourkit Profiler) can be used to measure the build time steps. It reports the timing for Maven mojos, Ant goals, etc. You need to register it as a Java agent for your build. It allows easy comparison of multiple builds, therefore it's really nice for trying different optimizations and configurations. Be aware the free license is only available for local builds, not for CI servers.

To get started with YouMonitor, you need to install and run the application. It will ask you for a repository, which is how you aggregate builds (e.g. use one repository per different project that you want to investigate). Afterward, select the [Monitoring in IDE or command line](https://www.yourkit.com/docs/youmonitor/help/ide_and_command_line.jsp) and use the button `Open Instructions`. That will show you the project and machine-specific argument which needs to be added to the Java command line. For example, if you want to profile tests, you might want to add it to the [argLine configuration of Tycho Surefire](https://www.eclipse.org/tycho/sitedocs/tycho-surefire-plugin/test-mojo.html#argLine).

## Contact

Contact the project developers via the project's "dev" list: https://dev.eclipse.org/mailman/listinfo/tycho-dev.

You can also post questions in the [Discussions](https://github.com/eclipse-tycho/tycho/discussions) section of this repository.

## 👔 Process and Legal

### Eclipse Development Process

This Eclipse Foundation open project is governed by the Eclipse Foundation
Development Process and operates under the terms of the Eclipse IP Policy.

### Eclipse Contributor Agreement

Before your contribution can be accepted by the project team contributors must
electronically sign the Eclipse Contributor Agreement (ECA): https://www.eclipse.org/legal/ECA.php

For more information, please see the Eclipse Committer Handbook:
https://www.eclipse.org/projects/handbook/#resources-commit


