# Target platform

The target platform is the set of artifacts from which Tycho resolves the project's dependencies.

Background: OSGi allows to specify dependencies with version ranges and package dependencies (Import-Package).
These dependencies (intentionally) do not map to unique artifacts.
In order to pick a set of concrete bundles to be used for compilation, test execution, and assembly, Tycho needs a set of candidate artifacts which may be used to match the dependencies.
This list of candidate artifacts is called the "target platform".
The process of selecting artifacts from the target platform according to the projects dependencies is called "dependency resolution".

There are different ways to define the content of the target platform; the most common is the usage of target definition files for more fine-grained control or the usage of repositories with layout=p2 in the POM, which add entire p2 repositories to the target platform.

## Which approach shall I use for the target platform of my project?

There are a few different ways to configure a target platform in Tycho.
These rules of thumb should help you to pick the right approach for your project:

* Prefer using a target definition file in Eclipse and Tycho to share the same target platform configuration between Tycho and Eclipse. This also gives you fine-grained control of the used artifacts and speeds up the build because fewer bundles in the target platform lead to a faster download and dependency resolution.
* If you want to get your Tycho build up and running quickly, just configure the needed p2 repositories in the POM and have Tycho pick anything required from these repositories. A good starting point should be to add one of the Simultaneous Release repositories.

## Target platform configuration

The target platform is defined through POM configuration (see details below).
Each module can have its own target platform, although with the normal configuration inheritance in Maven, the target platform configurations are usually the same across multiple modules.

### Simple target platform configuration

In order to allow Tycho to resolve the project dependencies against anything from a specific p2 repository, add that repository in the `<repositories>` section of the POM.

Example:

```xml
<repository>
   <id>eclipse-indigo</id>
   <url>http://download.eclipse.org/releases/2023-12</url>
   <layout>p2</layout>
</repository>
```

In terms of the target platform, this means that the entire content of the specified p2 repositories becomes part of the target platform.

Background: In a normal (i.e. non-Tycho) Maven project, one can configure Maven repositories which can be used by Maven to resolve the project dependencies.
Tycho can use p2 repositories for resolving OSGi dependencies.
The p2 repositories need to be marked with layout=p2. (The normal Maven dependency resolution ignores repositories with layout=p2.)

### Target files

The PDE target definition file format (`*.target`) allows to select a subset of units (bundles, features, etc.).

In order to add the content of a target definition file (see "Content" tab of the Target Editor) to the target platform in the Tycho build, place the target file in a eclipse-target-definition module and configure it in the target-platform-configuration build plugin. Example:

```xml
<plugin>
   <groupId>org.eclipse.tycho</groupId>
   <artifactId>target-platform-configuration</artifactId>
   <version>${tycho-version}</version>
   <configuration>
      <target>
         <artifact>
            <groupId>org.example</groupId>
            <artifactId>target-definition</artifactId>
            <version>1.0.0-SNAPSHOT</version>
         </artifact>
      </target>
   </configuration>
</plugin>
```

Since Tycho 0.17.0, it is also possible to configure multiple target files by specifying more than one `<artifact>` reference. Tycho interprets these target files independently and in the same way as in Eclipse: Each of the configured target files need to resolve successfully when opened in the Eclipse Target Editor. Note that the use of this Tycho feature is limited because the Eclipse PDE currently does not support activating more than one target file at once (see bug 392652).

Note: Tycho's interpretation of the target definition file format differs from the PDE in the following aspects:

The selection on the Content tab of the Target Editor is ignored.
See below for an alternative way to remove individual bundles from the target platform.
The option "Include source if available" is considered only if target-platform-configuration parameter targetDefinitionIncludeSource is set to honor (default value).
If targetDefinitionIncludeSource is set to force then available sources are always included and if set to ignore then available sources are always ignored.

### Extra requirements

See https://ci.eclipse.org/tycho/job/tycho-sitedocs/lastSuccessfulBuild/artifact/target/staging/target-platform-configuration/target-platform-configuration-mojo.html#dependency-resolution

### POM dependencies consider

See https://www.eclipse.org/tycho/sitedocs/target-platform-configuration/target-platform-configuration-mojo.html#pomDependencies.

For an example, see the POM of this [demo project](https://github.com/eclipse-tycho/tycho/tree/master/demo/itp02/build02).

## Effective content of the target platform

In case multiple target platform configuration approaches are combined, the target platform contains the union of the content defined through each approach.

Apart from the explicitly configured content, the target platform also contains the following artifacts:

* Other artifacts from the same reactor
* Locally built artifacts in the local Maven repository
* Finally, it is possible to remove artifacts again from the target platform through a filtering syntax.

### Locally built artifacts

Just like in a normal Maven build, a Tycho build can use artifacts that have been built locally and installed (e.g. with mvn clean install) into the local Maven repository.
In terms of the target platform, this means that these artifacts are implicitly added to the target platform. This is for example useful if you want to rebuild a part of a Tycho reactor, or if you want to build against a locally built, newer version of an upstream project.

There are the following options to disable this feature:

Setting the CLI option `-Dtycho.localArtifacts=ignore` excludes locally built artifacts in one build. (`tycho.localArtifacts=ignore` may also be configured in the settings.xml; in this case, the default behaviour can be temporarily re-enabled with the CLI option -Dtycho.localArtifacts=default.)
Deleting `~/.m2/repository/.meta/p2-local-metadata.properties` resets Tycho's list of locally build artifacts, and therefore these artifacts will not be added to target platforms (unless, of course, the artifacts are installed again).

### Filtering

See https://ci.eclipse.org/tycho/job/tycho-sitedocs/lastSuccessfulBuild/artifact/target/staging/target-platform-configuration/target-platform-configuration-mojo.html#filters

### Dependency resolution troubleshooting

Run mvn with the flags `-Dtycho.debug.resolver=true` and `-X` to see debug output.

This will debug

* Properties
* Available IUs
* JRE IUs
* Root IUs

### Listing IUs available

To list all the available IUs in an Eclipse p2 repository, run:

```shell
java -jar plugins/org.eclipse.equinox.launcher_1.6.600.v20231106-1826.jar -debug -consolelog -application org.eclipse.equinox.p2.director -repository https://download.eclipse.org/releases/latest/ -list
```

Java is used (instead of the eclipse binary) so that the console output appears in the shell.
Make sure your shell is inside the Eclipse root directory.

You will need to replace that version number for org.eclipse.equinox.launcher with the one found inside your Eclipse installation.
You will need to replace the Eclipse repository with the one you want a list of.
This can be used to double check availability of bundle versions, and compare with what Tycho searches for.

### Browsing a p2 repository

In Eclipse, open the "Repository Explorer" view.
If it is not available, then please install Oomph first: https://projects.eclipse.org/projects/tools.oomph/downloads

