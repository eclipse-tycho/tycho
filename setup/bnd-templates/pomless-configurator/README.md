# Tycho Workspace template fragment for bndtools for pomless Maven build

This is a [bndtools template fragment](https://bnd.bndtools.org/chapters/620-template-fragments.html) which you can use in a new or existing bndtools workspace, 
to add a Maven build based on Eclipse Tycho for your bnd workspace.

It creates a `.mvn` folder in your bnd workspace root, contain an `extensions.xml` and a `maven.config` and a `pom.xml` in your `cnf` folder for further configuration of the build.

See documentation for the [Tycho BND Plugin](https://tycho.eclipseprojects.io/doc/main/BndBuild.html) for more information. 

## Building your workspace

```
cd mybndworkspace
mvn clean install
```

This is all you need to build your bnd workspace with Maven / Tycho.

It will automatically consider all `bnd.bnd` files.


## the configurator pom.xml

While the workspace build is pomless, you can configure additional aspects of your build in the configurator `pom.xml`
in your `cnf` folder.

## Create executable jar based on .bndrun files

Tycho can also create an executable `.jar` file of your application based on the `.bndrun` file containing `-runbundles`, which you can then deploy and start.

Just add the following line to your `.mvn/maven.config` file or pass it on the maven commandline:

```
-Dbndrun.exports=mydemo.app
```

See [Create executable jar](https://tycho.eclipseprojects.io/doc/main/BndBuild.html#Create_executable_jar) for details.

