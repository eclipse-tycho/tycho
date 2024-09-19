# Tycho CI Friendly Versions

Starting with Maven 3.8.5 Tycho now supports an enhanced form of the [Maven CI Friendly Versions](https://maven.apache.org/maven-ci-friendly.html) beside the standard properties names one could also use:

- releaseVersion
- major
- minor
- micro
- qualifier

These uses the usual semantics that you can use them in a version string e.g. `<version>${releaseVersion}${qualifier}</version>` and pass them on the commandline.

Beside this, Tycho supports some useful default calculation for `qualifier` if you give a format on the commandline with `-Dtycho.buildqualifier.format=yyyyMMddHHmm` 
(or [any other format supported](https://tycho.eclipseprojects.io/doc/latest/tycho-packaging-plugin/build-qualifier-mojo.html#format)). Tycho will also make the build qualifier available in your Maven model!

Alternatively, if you want that your qualifier matches the one from maven you can specify `-DforceContextQualifier=abc`

That way you can configure your pom in the following way:

```
<project>
	<modelVersion>4.0.0</modelVersion>
	<groupId>...</groupId>
	<artifactId>...</artifactId>
	<packaging>pom</packaging>
	<version>${releaseVersion}${qualifier}</version>
  <properties>
    <!-- Defines the default Qualifier if no format is given-->
    <releaseVersion>1.0.0</releaseVersion>
    <qualifier>-SNAPSHOT</qualifier>
    ...
  </properties>
  ...
</project>
```

What will result in the usual `1.0.0-SNAPSHOT` for a regular `mvn clean install`, if you want to do a release, you can now simply call `mvn -Dtycho.buildqualifier.format=yyyyMMddHHmm clean deploy`
and your artifact will get the `1.0.0-<formatted qualifier>` version when published! This also is supported if you use pomless build.

To use this new feature you need to enable the tycho-build extension with the `.mvn/extensions.xml` file in the root of your project directory:

```
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

```
-Dtycho-version=4.0.0
# probably add more here ..
```

You can now control your Tycho version for `.mvn/extensions.xml` and your `pom.xml` in one place and still override it on the commandline with `-Dtycho-version=...`
