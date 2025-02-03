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
        - `myapp.bndrun` - a .bndrun file which can be used to export an executable .jar as part of the build
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

#### Create executable jar

Tycho can also create an executable `.jar` file of your application based on a `.bndrun` file containing `-runbundles`, which you can then deploy and start.

Just add the following line to your `.mvn/maven.config` file or pass it on the maven commandline:

```
-Dbndrun.exports=mydemo.app
```

This automatically searches all your bundles for a file named `mydemo.app.bndrun`.

If you have a bundle `myappbundle` which contains `mydemo.app.bndrun`, then the build would create an executable jar at the following location:

`/myappbundle/target/executable/mydemo.app.bndrun.jar`

Your build output will look like this:

```
[INFO] --- tycho-bnd:4.0.11:run (build) @ myappbundle ---
[INFO] Exporting mydemo.app.bndrun ...
[INFO] Exported to .../myappbundle/target/executable/tycho.demo.app.jar
```


## Mixed Builds

You can even combine a BND Workspace and PDE bundles in a build, see [demo](https://github.com/eclipse-tycho/tycho/tree/master/demo/bnd-pde-workspace).


## Troubleshooting

Note: All properties in `.mvn/maven.config` can also be supplied via command line, for example to use a different tycho version use the following command line argument:

`mvn clean install -Dtycho-version=5.0.0-SNAPSHOT`

This uses the snapshot build which is useful if you build tycho yourself on your local machine and want to test that build.


### polyglot.dump.pom

E.g., `-Dpolyglot.dump.pom=pom-gen.xml` specifies that the generated pom.xml files which tycho polyglot creates is called `pom-gen.xml`. 
This might be needed if you have name clashes with existing files in your build.


### tycho.pomless.aggregator.names

The command line argument `-Dtycho.pomless.aggregator.names=_dummydisabled_` is for handling a rare edge case: 
In case you have your bnd workspace in a subfolder named `bundles`, `plugins`, `tests`,`features`, `sites`,`products`, or `releng` then tycho can have problems.
The reason is that those names are kind of 'magic' names reserved for special usecases. 
`-Dtycho.pomless.aggregator.names=_dummydisabled_` is a workaround to instruct Tycho to ignore those names. You can choose any word like `_dummydisabled_`, but just make sure you do not have a folder with that name.

