# Tycho Build Extension

The `tycho-build` Maven extension is a core component of Tycho that enables advanced build capabilities including pomless builds, enhanced dependency resolution, and CI-friendly versioning.

## What does the tycho-build extension do?

The `tycho-build` extension provides the following key features:

1. **Pomless Build Support**: Enables building Eclipse/OSGi projects and BND workspaces without requiring a `pom.xml` file in every module
2. **Enhanced Dependency Graph Building**: Optimizes the Maven reactor build order based on Eclipse/OSGi dependencies
3. **CI-Friendly Versioning**: Supports enhanced version properties for continuous integration workflows
4. **Smart Builder Integration**: Automatically enables the Maven smart builder for parallel builds when appropriate

## When should you use the tycho-build extension?

You should use the `tycho-build` extension when:

- You want to use **pomless builds** for Eclipse plugins, features, products, or BND projects
- You want to leverage **Tycho CI-friendly versioning** with dynamic qualifiers
- You are building a project with a **structured build layout** and want to minimize Maven configuration

## How to enable the tycho-build extension

To enable the `tycho-build` extension in your project:

1. Create a `.mvn` folder in the root of your project (the multi-module project directory)

2. Create a file called `extensions.xml` inside the `.mvn` folder with the following content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
   <extension>
      <groupId>org.eclipse.tycho</groupId>
      <artifactId>tycho-build</artifactId>
      <version>${tycho-version}</version>
   </extension>
</extensions>
```

3. Create a file called `maven.config` in the `.mvn` folder to define the Tycho version:

```properties
-Dtycho-version=4.0.10
```

Make sure to adjust the version to match the [latest Tycho release](https://github.com/eclipse-tycho/tycho/releases).

## What happens when the extension is loaded?

When Maven starts, the `tycho-build` extension is loaded before the build begins. It:

1. Registers polyglot Maven mappings that allow Tycho to read Eclipse/OSGi metadata (MANIFEST.MF, feature.xml, *.product, etc.) and BND configuration files (bnd.bnd, *.bndrun) as Maven models
2. Provides a custom dependency graph builder that understands Eclipse/OSGi dependencies
3. Enables advanced features like build timestamp providers and CI-friendly version properties

## Related Documentation

- [Structured Build Layout and Pomless Builds](StructuredBuild.html) - Learn how to use pomless builds with Eclipse plugin projects
- [BND Workspace and Pomless Builds](BndBuild.html) - Learn how to build BND workspaces with Tycho
- [Tycho CI Friendly Versions](TychoCiFriendly.html) - Learn about enhanced versioning capabilities

## Examples

You can find working examples of projects using the `tycho-build` extension in the Tycho repository:

- [BND Workspace Demo](https://github.com/eclipse-tycho/tycho/tree/master/demo/bnd-workspace)
- [Mixed BND and PDE Workspace Demo](https://github.com/eclipse-tycho/tycho/tree/master/demo/bnd-pde-workspace)
- [PDE Structured Build Demo](https://github.com/eclipse-tycho/tycho/tree/master/demo/pde-automatic-manifest)

## Migration from tycho-pomless

If you are currently using the older `org.eclipse.tycho.extras:tycho-pomless` extension, you should migrate to `org.eclipse.tycho:tycho-build`. Simply update your `.mvn/extensions.xml` file to reference the new extension as shown above. The `tycho-pomless` extension is deprecated and will be removed in a future version of Tycho.
