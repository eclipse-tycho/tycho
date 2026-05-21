# Packaging Types

Tycho defines the following custom Maven packaging types targeted for Eclipse Plug-in development.

* `eclipse-plugin` corresponds to [Eclipse Plug-in and Plug-in Fragment projects](https://github.com/eclipse-pde/eclipse.pde/blob/master/docs/User_Guide.md#Plug-in).
* `eclipse-test-plugin` is similar to `eclipse-plugin` but only contains Plugin Tests to be executed inside an OSGi runtime. There is a notable difference between `eclipse-plugin` and `eclipse-test-plugin` with respect to the goal executed during `integration-test`. The former uses `tycho-surefire-plugin:integration-test` while the latter uses `tycho-surefire-plugin:test`.
* `eclipse-feature` corresponds to [Eclipse Feature projects](https://github.com/eclipse-pde/eclipse.pde/blob/master/docs/User_Guide.md#Feature)
* `eclipse-repository` corresponds to projects containing a `category.xml` file or `.product` files for building Eclipse products. The support of [Eclipse Update Site projects](https://github.com/eclipse-pde/eclipse.pde/blob/master/docs/User_Guide.md#Update-Site) has been removed in latest Tycho versions. See [Building Products](Products.html) for information on building Eclipse RCP applications.
* `eclipse-target-definition` corresponds to [Eclipse Target Platform](https://github.com/eclipse-pde/eclipse.pde/blob/master/docs/User_Guide.md#Target_Platform)
* `p2-installable-unit` corresponds to [Installable Units](https://github.com/eclipse-equinox/p2/blob/master/docs/Installable_Units.md)

The lifecycle bindings (i.e. which Maven plugins are executed in which Maven phase by default) are defined by `tycho-maven-plugin` in a [Maven extension](https://maven.apache.org/guides/mini/guide-using-extensions.html) therefore it needs to be loaded accordingly:

```xml
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

```xml
<plugin>
   <groupId>org.apache.tycho</groupId>
   <artifactId>p2-maven-plugin</artifactId>
   <version>${project.version}</version>
   <extensions>true</extensions>
</plugin>
```