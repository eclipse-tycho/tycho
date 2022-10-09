# Packaging Types

Tycho defines the following custom Maven packaging types targeted for Eclipse Plug-in development.

1. `eclipse-plugin` corresponds to [Eclipse Plug-in and Plug-in Fragment projects](https://wiki.eclipse.org/PDE/User_Guide#Plug-in)
1. `eclipse-test-plugin` is similar to `eclipse-plugin` but only contains ITs to be executed inside an Eclipse application. There is a noticable difference between `eclipse-plugin` and `eclipse-test-plugin` with respect to the goal executed during `integration-test`. The former uses `tycho-surefire-plugin:integration-test` while the latter uses `tycho-surefire-plugin:test`.
1. `eclipse-feature` corresponds to [Eclipse Feature projects](https://wiki.eclipse.org/PDE/User_Guide#Feature)
1. `eclipse-repository` corresponds to [Eclipse Update Site projects](https://wiki.eclipse.org/PDE/User_Guide#Update_Site)
1. `eclipse-target-definition` corresponds to [Eclipse Target Platform](https://wiki.eclipse.org/PDE/User_Guide#Target_Platform)
1. `p2-installable-unit` corresponds to [Installable Units](https://wiki.eclipse.org/Installable_Units)

The lifecycle bindings (i.e. which Maven plugins are executed in which Maven phase by default) are defined by `tycho-maven-plugin` in a [Maven extension](https://maven.apache.org/guides/mini/guide-using-extensions.html) therefore it needs to be loaded accordingly:

```
<plugin>
  <groupId>org.apache.tycho</groupId>
  <artifactId>tycho-maven-plugin</artifactId>
  <version>${project.version}</version>
  <extensions>true</extensions>
</plugin>
```

All bindings are defined in <https://github.com/eclipse-tycho/tycho/blob/master/tycho-maven-plugin/src/main/resources/META-INF/plexus/components.xml>.
Only the `default` lifecycle has custom bindings, i.e. the [`clean` and `site` lifecycles](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html#lifecycle-reference) behave as for every other packaging.

The according artifact handlers (i.e. the mapping from the packaging type to a specific extension) are provided by `p2-maven-plugin` in <https://github.com/eclipse-tycho/tycho/tree/master/p2-maven-plugin/src/main/java/org/eclipse/tycho/p2maven/repository>. When referencing one of the packaging types as Maven dependency it needs to be loaded with extensions as well:

```
<plugin>
  <groupId>org.apache.tycho</groupId>
  <artifactId>p2-maven-plugin</artifactId>
  <version>${project.version}</version>
  <extensions>true</extensions>
</plugin>
```