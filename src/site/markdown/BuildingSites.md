# Building P2 Update Sites

Tycho supports building Eclipse p2 update sites (repositories) through the `eclipse-repository` packaging type and the `tycho-p2-repository-plugin`.
A p2 repository is a structured collection of installable units (IUs) that Eclipse-based applications can use to install and update software components.

## Overview

Building a p2 update site with Tycho involves:

1. Creating a project with the `eclipse-repository` packaging type.
2. Defining what content to include using a `category.xml` file.
3. Configuring the `tycho-p2-repository-plugin` to control how the repository is assembled.

The result is a p2 repository that can be published to a web server or file system for users to install from.

**Note:** The `eclipse-repository` packaging type can also be used to build Eclipse products (RCP applications). See [Building Products](Products.html) for more information on product builds.

## Basic Project Setup

Create a project with the `eclipse-repository` packaging type:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>com.example.site</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>eclipse-repository</packaging>
</project>
```

## Defining Repository Content with category.xml

A `category.xml` file placed in the project root defines which content is placed into the p2 repository and how it is displayed in the p2 installation dialog.

### Simple Example

The following `category.xml` lists one feature and one plug-in under a single category:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<site>
   <feature id="com.example.feature">
      <category name="com.example.category"/>
   </feature>

   <bundle id="com.example.plugin"/>

   <category-def name="com.example.category" label="Example Components">
      <description>Components provided by the Example project.</description>
   </category-def>
</site>
```

In this example:

- The `<feature>` element includes a feature by its ID. A `<category>` child element assigns it to a named category.
- The `<bundle>` element directly includes a plug-in (OSGi bundle) without requiring it to be part of a feature.
- The `<category-def>` element defines the category that is displayed in the Eclipse installation dialog.

### Platform-Specific Features

Features can declare platform-specific compatibility for multi-platform builds:

```xml
<feature id="com.example.linux.feature" version="0.0.0" os="linux"/>
<feature id="com.example.win32.feature" version="0.0.0" os="win32"/>
<feature id="com.example.macosx.feature" version="0.0.0" os="macosx"/>
```

### Including Arbitrary Installable Units

You can include any installable unit (IU) directly:

```xml
<iu id="com.example.some.unit"/>
```

For more advanced inclusion patterns using p2 query expressions, see [Advanced Category Definitions](CategoryDefinitions.html).

## Assembling the Repository

The `tycho-p2-repository-plugin` assembles the final p2 repository. By default it only includes what is explicitly listed in `category.xml`.
Tycho provides several configuration options to control what additional content is pulled into the repository.
See [Controlling Repository Content](RepositoryContent.html) for a detailed comparison of all available options.

### Minimal Configuration

With default settings, only the IUs explicitly listed in `category.xml` are included:

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
</plugin>
```

### Self-Contained Repository

To create a repository that includes all transitive dependencies (so users do not need access to other repositories to install):

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <includeAllDependencies>true</includeAllDependencies>
  </configuration>
</plugin>
```

### Repository with Sources

To automatically include source bundles for all included content:

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <includeAllDependencies>true</includeAllDependencies>
    <includeAllSources>true</includeAllSources>
  </configuration>
</plugin>
```

## Repository References

Repository references tell the p2 installer where to find dependencies that are not included directly in the repository.
This is useful when you want to keep your repository small but still allow users to install your software without manually adding extra repositories.

### Explicit References in category.xml

You can declare repository references directly in `category.xml`:

```xml
<site>
   <repository-reference location="https://download.eclipse.org/releases/latest" enabled="true"/>
   <!-- features and categories... -->
</site>
```

### Automatic References from POM

Tycho can automatically add all p2 repositories declared in the POM as repository references:

```xml
<configuration>
  <addPomRepositoryReferences>true</addPomRepositoryReferences>
</configuration>
```

### Automatic References from Target Platform

Similarly, repositories from `InstallableUnit` locations in the target file can be added as references:

```xml
<configuration>
  <addIUTargetRepositoryReferences>true</addIUTargetRepositoryReferences>
</configuration>
```

### Filtering Repository References

When adding references automatically, you can filter which ones are included:

```xml
<configuration>
  <addPomRepositoryReferences>true</addPomRepositoryReferences>
  <repositoryReferenceFilter>
    <addOnlyProviding>true</addOnlyProviding>
    <exclude>
      <location>https://internal.example.com/**</location>
    </exclude>
  </repositoryReferenceFilter>
</configuration>
```

## Additional Options

| Option | Default | Description |
|--------|---------|-------------|
| `compress` | `true` | Compress `content.xml` and `artifacts.xml` index files |
| `xzCompress` | `true` | Create XZ-compressed index files for better compression |
| `keepNonXzIndexFiles` | `true` | Keep jar/xml index files alongside XZ files for backward compatibility |
| `repositoryName` | `${project.name}` | Name attribute in the created repository |
| `createArtifactRepository` | `true` | Create artifact files; set to `false` for a metadata-only repository |
| `generateOSGiRepository` | `false` | Generate an OSGi Repository alongside the p2 repository |

## Further Reading

- [Advanced Category Definitions](CategoryDefinitions.html) — Dynamic categories, p2 query expressions, and IU filtering
- [Controlling Repository Content](RepositoryContent.html) — Detailed comparison of all inclusion and exclusion options
- [Tycho P2 Repository Plugin Reference](tycho-p2-repository-plugin/plugin-info.html) — Complete reference for all configuration options
- [Packaging Types](PackagingTypes.html) — Information about the `eclipse-repository` packaging type
