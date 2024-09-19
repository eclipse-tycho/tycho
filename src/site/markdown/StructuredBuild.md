# Structured Build Layout and Pomless Builds

Tycho supports any layout of your build, but you can save you a lot of configuration work if you are using the so called **Structured Build Layout**.

## Structured Build Layout
A structured build layout usually has the following folder layout, even though you might not use all of the depending on your project:

- `root folder` - this usually contains your parent pom where you configure the plugins to use
    - `bundles` (or `plugins`) - this contains your bundles that make up your application
        - `bundle1`
        - `bundle2`
        - `...`
    - `features` - this folder will contain any features that structure your bundles into user installable units
        - `feature1`
        - `...`
    - `sites` - if you have any update-sites they go into this folder
        - `my-site`
        - `...`
    - `products` - the products to assemble are located here
        - `cool-product`
        - `...`
- `target-platform.target` the target platform that should be used

## Pomless Builds
Given the above layout, Tycho now has a good knowledge about what your build artifacts are.
In contrast to a traditional maven build where each module has to contain a `pom.xml` file Tycho can derive most if not all from your existing data, that is the files you are created and using in our IDE, 
there are only a few steps to consider:

1. Add a folder called `.mvn` to the root
2. Inside the `.mvn` folder place a file called `extensions.xml` with the following content:

```
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <extension>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-build</artifactId>
    <version>${tycho-version}</version>
  </extension>
</extensions>
```

3. create a file called `maven.config` in the `.mvn` folder with the following content (adjust the version accordingly!):

```
-Dtycho-version=3.0.0
```

4. finally create a `pom.xml` with the following content in the root folder:

```
<?xml version="1.0" encoding="UTF-8"?>
<project
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
	xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<modelVersion>4.0.0</modelVersion>
	<groupId> ... your desired group id ...</groupId>
	<artifactId>parent</artifactId>
	<version>1.0.0-SNAPSHOT</version>
	<packaging>pom</packaging>
	
	<modules>
		<module>bundles</module>
		<module>features</module>
		<module>sites</module>
		<module>products</module>
	</modules>
	
	<build>
		<plugins>
			<plugin>
				<groupId>org.eclipse.tycho</groupId>
				<artifactId>tycho-maven-plugin</artifactId>
				<version>${tycho-version}</version>
				<extensions>true</extensions>
			</plugin>
			<plugin>
				<groupId>org.eclipse.tycho</groupId>
				<artifactId>target-platform-configuration</artifactId>
				<version>${tycho-version}</version>
				<configuration>
					<target>
						<file>../../target-platform.target</file>
					</target>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

5. You can now run your build with `mvn clean verify`
