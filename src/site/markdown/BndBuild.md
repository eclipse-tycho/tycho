# BND Workspace Layout and Pomless Builds

Tycho supports building projects that use the [BND Workspace Layout](https://bndtools.org/concepts.html).

## BND Workspace Layout
A BND Workspace layout build layout usually has the following structure:

- `root folder` - this usually is the root of your project repository
    - `cnf` - configuration folder for general setup
        - `build.bnd` - main configuration file
        - `ext` - additional configuration (optional)
    - `bundle1` - A bundle project
        - `bnd.bnd` - project configuration file
    - `bundle2` - Another bundle project
        - `bnd.bnd` - project configuration file
    - `...`

Any folder that does not match the layout is ignored.

## Pomless Builds
Given the above layout, Tycho now has a good knowledge about what your build artifacts are.
In contrast to a traditional maven build where each module has to contain a `pom.xml` file Tycho can derive most all from your supplied bnd configuration files, so everything is configured there and usually no additional maven configuration is required, therefore this build is completely pomless (no `pom.xml`), there are only a few steps to consider:

- Add a folder called `.mvn` to the root
- Inside the `.mvn` folder place a file called `extensions.xml` with the following content:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
   <extension>
      <groupId>org.eclipse.tycho</groupId>
      <artifactId>tycho-build</artifactId>
      <version>${tycho-version}</version>
   </extension>
</extensions>
```

- create a file called `maven.config` in the `.mvn` folder with the following content (adjust the Tycho version accordingly to the [latest release](https://github.com/eclipse-tycho/tycho/releases)!):
```properties
-Dtycho-version=4.0.10
```

- You can now run your build with `mvn clean verify`.

You can check more details in a [demo project](https://github.com/eclipse-tycho/tycho/tree/master/demo/bnd-workspace).

### Configure the pomless build

If you want to further configure the build can be done in these ways:

1. You can specify additional global properties in the `.mvn/maven.config`.
2. You can define properties per project properties in the `bnd.bnd` file `pom.model.property.<some property>: true`, see [the wiki](https://github.com/eclipse-tycho/tycho/wiki/Tycho-Pomless#overwrite-group-and-artifact-ids) for more details.
3. You can place a `pom.xml` in your `cnf` folder this will then be used as a parent for the aggregator, here you can add additional mojos, profiles and so on. If you want to enable certain things only for some of the projects you can use properties as described in (2) to skip the execution of mojos not relevant for other projects.

## Mixed Builds

You can even combine a BND Workspace and PDE bundles in a build, see [demo](https://github.com/eclipse-tycho/tycho/tree/master/demo/bnd-pde-workspace).
