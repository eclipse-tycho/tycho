## Build Properties

Tycho uses the `build.properties` file [as defined by PDE](http://help.eclipse.org/luna/index.jsp?topic=/org.eclipse.pde.doc.user/reference/pde_feature_generating_build.htm) to configure various aspects of the build.

Note that Tycho only supports a subset of keys defined by PDE. If a key is not supported, this may be because

* it's legacy/deprecated
* it doesn't fit into the maven project model or the way maven is generally expected to work
* there are other ways to achieve the desired configuration (e.g. using pom.xml)
* it's a missing feature

In addition to PDE, Tycho supports using maven property expressions like `${project.version}` in `build.properties` values. Note that these expressions will only be replaced in a Tycho build, not when using the eclipse IDE incremental build.  
See the table below for which keys in `build.properties` defined by PDE are supported by Tycho or if not, whether there are alternatives when using Tycho.

### Common Properties

Key | Value
--- | ---
bin.includes | supported
bin.excludes | supported
qualifier    | not supported - use [Build Qualifer Mojo](tycho-packaging-plugin/build-qualifier-mojo.html) instead
forceContextQualifier | supported
custom*      | not supported

### Plugin-specific properties

Key | Value
--- | ---
source.library | supported
output.library | not supported - compiler output goes to `${project.build.outputDirectory}` (usually `target/classes`) for main jar and `target/nestedjar-classes` for nested jars
exclude.library | supported
extra.library | supported
manifest.library | supported
src.includes | supported
src.excludes | supported
jars.extra.classpath | only supported in the form `platform:/plugin/<Bundle-SymbolicName>[/path/to/nested/jar]`
jars.compile.order | supported


### Compiler-specific properties

Key | Value
--- | ---
jre.compilation.profile | supported
javacSource | supported
javacTarget | supported
bootClasspath | not supported - use [maven toolchains](tycho-compiler-plugin/compile-mojo.html#useJDK) instead
javacWarnings.library | not supported - use [compilerArgument](https://wiki.eclipse.org/Tycho/FAQ#How_to_configure_warning.2Ferror_settings_of_the_OSGi_compiler.3F) instead.
javacErrors.library | not supported - use [compilerArgument](https://wiki.eclipse.org/Tycho/FAQ#How_to_configure_warning.2Ferror_settings_of_the_OSGi_compiler.3F) instead.
javacDefaultEncoding.library | supported
javacCustomEncodings.library | not supported
javacProjectSettings | not supported. Use [useProjectSettings](tycho-compiler-plugin/compile-mojo.html#useProjectSettings) or [compilerArgs](tycho-compiler-plugin/compile-mojo.html#compilerArgs) instead. 
compilerArg | not supported. Use [compilerArgs](tycho-compiler-plugin/compile-mojo.html#compilerArgs) instead
compilerAdapter | not supported. Use [compilerId](tycho-compiler-plugin/compile-mojo.html#compilerId) instead
compilerAdapter.useLog | not supported
compilerAdapter.useArgFile | not supported
sourceFileExtensions | not supported - use [excludeResources](tycho-compiler-plugin/compile-mojo.html#excludeResources) instead

### Feature-specific properties

Key | Value
--- | ---
root | supported
root.config | supported
root.folder | supported (since Tycho 0.27.0)
root.config.folder | supported (since Tycho 0.27.0)
root.permissions | supported
root.link | supported
generate.feature | not supported - use [Tycho Source Plugin](tycho-source-plugin/plugin-info.html) instead
generate.plugin | not supported - use [Tycho Source Plugin](tycho-source-plugin/plugin-info.html) instead
significantVersionDigits | not supported
generatedVersionLength | not supported



