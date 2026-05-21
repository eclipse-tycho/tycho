# Controlling Repository Content

The `tycho-p2-repository-plugin` provides several configuration options that control which installable units (IUs) and artifacts end up in the assembled p2 repository.
This page explains each option, shows how to configure it, and discusses the trade-offs to help you choose the right approach for your project.

## Overview of Options

| Option | Default | Effect |
|--------|---------|--------|
| [`includeAllDependencies`](#includeAllDependencies) | `false` | Include all transitive dependencies to make the repository self-contained |
| [`includeAllSources`](#includeAllSources) | `false` | Include source bundles for all included content |
| [`includeRequiredPlugins`](#includeRequiredPlugins) | `false` | Include plugins listed in feature dependencies (not just inclusions) |
| [`includeRequiredFeatures`](#includeRequiredFeatures) | `false` | Include features listed in feature dependencies (not just inclusions) |
| [`filterProvided`](#filterProvided) | `false` | Exclude IUs that are already available in referenced repositories |

These options are configured on the `tycho-p2-repository-plugin` in the `<configuration>` section.

<a id="includeAllDependencies"></a>
## includeAllDependencies

When set to `true`, the repository will include all transitive dependencies required by the IUs listed in `category.xml`.
This makes the repository **self-contained**: users can install from it without needing access to any other repository.

### Configuration

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

### When to Use

- **Use** when you want users to install from your repository without configuring additional update sites.
- **Use** when distributing a repository for offline installation.

### Trade-offs

| Pros | Cons |
|------|------|
| Users only need a single repository URL | Repository size can grow significantly |
| Works for offline installations | May include platform dependencies (e.g., Eclipse runtime bundles) that users already have |
| Simplest setup for consumers | Build time increases as more artifacts are packaged |

### Combining with filterProvided

A common pattern is to combine `includeAllDependencies` with `filterProvided` and repository references to create a repository that contains only your project's unique dependencies while still pointing users to where they can find the rest:

```xml
<configuration>
  <includeAllDependencies>true</includeAllDependencies>
  <filterProvided>true</filterProvided>
  <addPomRepositoryReferences>true</addPomRepositoryReferences>
</configuration>
```

This first resolves all transitive dependencies, then removes any that are already available in the referenced repositories.
The result is a **minimal self-contained repository** that only ships what is not available elsewhere.

<a id="includeAllSources"></a>
## includeAllSources

When set to `true`, the repository will include source bundles for all included IUs where source bundles are available.
By default, only source bundles that are explicitly listed in `category.xml` are included.

### Configuration

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <includeAllSources>true</includeAllSources>
  </configuration>
</plugin>
```

### When to Use

- **Use** when you want to provide source bundles for debugging and development.
- **Use** in combination with `includeAllDependencies` to also include sources of third-party dependencies.

### Trade-offs

| Pros | Cons |
|------|------|
| Enables source-level debugging of installed bundles | Approximately doubles repository size |
| Useful for SDK-style distributions | Not needed for end-user repositories |

<a id="includeRequiredPlugins"></a>
## includeRequiredPlugins

Controls whether plugins that are declared as **dependencies** (required) in features are included, in addition to plugins that are listed as **inclusions**.

By default, Tycho only includes plugins that a feature explicitly **includes**. Plugins listed in `<requires>` are not packaged into the repository since they are expected to be available from other repositories at install time.

When set to `true`, required plugins are also included in the repository.

**Note:** Plugins with **strict version ranges** (e.g., `[1.0.0, 1.0.0]`) are always included regardless of this setting.

### Configuration

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <includeRequiredPlugins>true</includeRequiredPlugins>
  </configuration>
</plugin>
```

### When to Use

- **Use** when your features use `<requires>` to declare plugin dependencies and you want those plugins in the repository.
- **Avoid** if you want a minimal repository that relies on other repositories for required plugins.

<a id="includeRequiredFeatures"></a>
## includeRequiredFeatures

Similar to `includeRequiredPlugins`, but for features. When set to `true`, features declared as **dependencies** in other features are included in the repository.

**Note:** Features with **strict version ranges** are always included regardless of this setting.

### Configuration

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <includeRequiredFeatures>true</includeRequiredFeatures>
  </configuration>
</plugin>
```

### When to Use

- **Use** when your features reference other features via `<requires>` and you want a more complete repository.
- **Avoid** if the required features are provided by well-known repositories (e.g., Eclipse Platform) that users already have configured.

<a id="filterProvided"></a>
## filterProvided

When set to `true`, the repository assembly process removes any IUs and artifacts that are already available in **referenced repositories**.
This is evaluated after all inclusions are resolved, so it acts as a final filter.

Referenced repositories are those declared via:

- `<repository-reference>` elements in `category.xml`
- `addPomRepositoryReferences` (which adds repositories from the POM)
- `addIUTargetRepositoryReferences` (which adds repositories from target files)

### Configuration

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <includeAllDependencies>true</includeAllDependencies>
    <filterProvided>true</filterProvided>
    <addPomRepositoryReferences>true</addPomRepositoryReferences>
  </configuration>
</plugin>
```

### When to Use

- **Use** when you want a minimal repository that only ships content not available elsewhere.
- **Use** in combination with `includeAllDependencies` to first resolve all dependencies, then remove what is already provided.
- This is particularly useful when you build against a platform repository (e.g., Eclipse releases) and only want to ship your own bundles and their unique third-party dependencies.

### Trade-offs

| Pros | Cons |
|------|------|
| Produces minimal repositories | Requires that referenced repositories remain available at install time |
| Avoids shipping platform dependencies your users already have | Users must configure the referenced repositories or have them available |
| Reduces repository size and build time | Repository content changes if referenced repositories change |

## Common Recipes

### Recipe: Ship Only Your Own Bundles

You want to build a repository containing only your project's bundles. Dependencies from the Eclipse platform or other well-known repositories should not be included. Users are expected to already have the Eclipse platform available.

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <addPomRepositoryReferences>true</addPomRepositoryReferences>
  </configuration>
</plugin>
```

With this setup, only the features and bundles explicitly listed in `category.xml` are included. Repository references from the POM are added so the p2 installer knows where to find the dependencies.

### Recipe: Self-Contained Repository Without Platform Dependencies

You want all dependencies resolved and included, except for those already available from the Eclipse platform.

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <includeAllDependencies>true</includeAllDependencies>
    <filterProvided>true</filterProvided>
    <addPomRepositoryReferences>true</addPomRepositoryReferences>
  </configuration>
</plugin>
```

This resolves all transitive dependencies, then filters out anything that comes from the POM repositories (typically the Eclipse platform). The result contains only your project's bundles and their unique third-party dependencies.

### Recipe: Full SDK Repository with Sources

You want a complete, self-contained repository including source bundles for development use.

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

### Recipe: Include Feature Dependencies

Your features use `<requires>` to declare dependencies on plugins and other features, and you want those included.

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-repository-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <includeRequiredPlugins>true</includeRequiredPlugins>
    <includeRequiredFeatures>true</includeRequiredFeatures>
  </configuration>
</plugin>
```

## Further Reading

- [Building P2 Update Sites](BuildingSites.html) — Overview and basic setup
- [Advanced Category Definitions](CategoryDefinitions.html) — Dynamic categories and p2 query expressions
- [Tycho P2 Repository Plugin Reference](tycho-p2-repository-plugin/plugin-info.html) — Complete reference for all configuration options
