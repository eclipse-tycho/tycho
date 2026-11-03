Target Platform Demo
====================

This demo shows features of the [target platform](https://tycho.eclipseprojects.io/doc/latest/TargetPlatform.html) support in Tycho.
Everything in here is intentionally minimal and has no special meaning, the interesting part is _how_ the pieces are connected.

Currently it shows:

- how `${project_loc:...}` references in a `.target` file are resolved

Contents
--------

- `tycho.demo.target` - the target definition (pomless), start reading here
- `locations/by-project-name` - a project found by its project name
- `locations/by-artifact-id` - a project found by its Maven artifactId
- `locations/by-directory-name` - a project found by the name of its folder
- `tycho.demo.bundle` - a small bundle (pomless) that is built against the target, so the target is actually resolved

Run it with `mvn clean verify`.

`${project_loc:...}` references
-------------------------------

A `Directory` location in a target can point to a folder of another project in the same build, e.g. `${project_loc:my.project}/plugins`.
In the Eclipse IDE the name refers to the project name in the workspace (the `name` in the `.project` file).
Maven has no such file, therefore Tycho looks for a project in the reactor in the following order and takes the first match:

1. the project name (`<name>` of the `pom.xml`) - keep this the same as the name in the `.project` file so that IDE and Maven agree
2. the Maven `artifactId`
3. the name of the folder that contains the project

A leading slash (`${project_loc:/my.project}`) is accepted as well, as the IDE writes it this way.

If nothing matches, Tycho logs a warning and leaves the reference as it is.
This can be intentional, e.g. if a project is not part of the current build, but it means the target might be incomplete.
This demo has no such case, so it builds without any warning.

See the [target platform documentation](https://tycho.eclipseprojects.io/doc/latest/TargetPlatform.html) for more about target definitions.
