## Tycho Properties

Maven provides a set of properties like `project.version` or `project.basedir` that could be used within pom files e.g. to configure the plugins.
Tycho provides its own set of additional properties.


### Tycho Environment Properties

These properties are set based on the platform where Tycho gets executed:

Name | Value
--- | ---
tycho.env.osgi.ws | the platform windowing system e.g. `gtk`
tycho.env.osgi.os | the platform operating system e.g. `linux`
tycho.env.osgi.arch | the platform architecture e.g. `x86_64`





