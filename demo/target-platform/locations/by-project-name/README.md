By project name
===============

Referenced from the target as `${project_loc:tycho.demo.by.project.name}` (and `${project_loc:/tycho.demo.by.project.name}`).

The reference is matched against the project name.
In the `pom.xml` this is the `<name>`, in the Eclipse IDE it is the name in the `.project` file.
Both are `tycho.demo.by.project.name` here, so the IDE and Maven find the same folder.
