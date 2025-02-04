# Tycho Workspace template fragment for bndtools for pomless Maven build

This is a [bndtools template fragment](https://bnd.bndtools.org/chapters/620-template-fragments.html) which you can use in a new or existing bndtools workspace, 
to add a Maven build based on Eclipse Tycho for your bnd workspace.

It creates a `.mvn` folder in your bnd workspace root, contain an `extensions.xml` and a `maven.config`.

See documentation for the [Tycho BND Plugin](https://tycho.eclipseprojects.io/doc/main/BndBuild.html) for more information. 

## Building your workspace

```
cd mybndworkspace
mvn clean install
```

This is all you need to build your bnd workspace with Maven / Tycho.

It will automatically consider all `bnd.bnd` files.


## Optional configurator pom.xml

While the default build is pomless, you can create a parent `pom.xml` in your `cnf` folder.
It can be as simple as the following:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.example.mygroup</groupId>
	<artifactId>parent</artifactId>
	<version>1.0.0-SNAPSHOT</version>
	<packaging>pom</packaging>
</project>
```

This is useful if you want to add other maven plugins to your build, e.g., for code analysis or reporting.

## Create executable jar based on .bndrun files

Tycho can also create an executable `.jar` file of your application based on the `.bndrun` file containing `-runbundles`, which you can then deploy and start.

Just add the following line to your `.mvn/maven.config` file or pass it on the maven commandline:

```
-Dbndrun.exports=mydemo.app
```

See [Create executable jar](https://tycho.eclipseprojects.io/doc/main/BndBuild.html#Create_executable_jar) for details.

