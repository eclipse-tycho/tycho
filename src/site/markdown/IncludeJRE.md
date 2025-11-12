# Including a JRE with Eclipse Products

When building Eclipse RCP applications, you often want to bundle a Java Runtime Environment (JRE) with your product. This ensures that end users don't need to have Java pre-installed on their systems and guarantees your application runs with a tested, compatible JRE version.

Tycho offers multiple approaches for including a JRE with your product, each with its own advantages and use cases.

## Overview of Available Methods

There are two main approaches to include a JRE with your Eclipse product:

1. **Automatic JRE Inclusion** - Using the `includeJRE="true"` flag in your product file (recommended for most use cases)
2. **Manual JRE Inclusion** - Including JRE features explicitly in your product definition

## Method 1: Automatic JRE Inclusion (Recommended)

The automatic approach uses the `includeJRE="true"` attribute in your `.product` file. When this flag is set, Tycho automatically handles JRE resolution and inclusion during the build process.

### How It Works

When `includeJRE="true"` is set in your product file:

1. Tycho looks for JRE installable units (IUs) in your target platform
2. It automatically resolves the appropriate JRE based on the product's target environments
3. The JRE is included in the final product materialization and archives

This approach leverages the p2 dependency resolution mechanism, making it the most integrated and streamlined solution.

### Prerequisites

To use automatic JRE inclusion, you only need:

1. Set `includeJRE="true"` in your product file

That's it! Tycho automatically fetches the JRE from the default [JustJ](https://www.eclipse.org/justj/) repository.

### Configuration Example

**Step 1: Set `includeJRE="true"` in your product file**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?pde version="3.5"?>

<product name="My Application" 
         uid="my.application.product" 
         id="my.application.product" 
         application="org.eclipse.ui.ide.workbench" 
         version="1.0.0" 
         type="features" 
         includeLaunchers="true" 
         includeJRE="true">
   
   <!-- Your product configuration -->
   <features>
      <feature id="org.eclipse.platform" installMode="root"/>
      <!-- Your other features -->
   </features>
   
</product>
```

That's all you need! When `includeJRE="true"` is set, Tycho automatically:
- Fetches the appropriate JRE from the JustJ repository (https://download.eclipse.org/justj/jres)
- Resolves the correct JRE version based on your product's target environments
- Includes the JRE in the final product

### Customizing the JRE Repository (Optional)

By default, Tycho uses the JustJ repository at `https://download.eclipse.org/justj/jres`. If you need to use a different JRE source or disable automatic JRE fetching, you can configure the `productRepository` parameter in the `tycho-p2-director-plugin`:

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-director-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <!-- Use a specific JustJ version repository -->
    <productRepository>https://download.eclipse.org/justj/jres/21/updates/release/</productRepository>
    
    <!-- Or disable automatic JRE fetching entirely -->
    <!-- <productRepository></productRepository> -->
  </configuration>
</plugin>
```

### Choosing a JustJ JRE Version

By default, Tycho uses the base JustJ repository (`https://download.eclipse.org/justj/jres`) which provides JREs for multiple Java versions. If you need a specific Java version, you can configure the `productRepository` parameter as shown above.

Common JustJ repository options:
- Default (all versions): `https://download.eclipse.org/justj/jres`
- Java 17 specific: `https://download.eclipse.org/justj/jres/17/updates/release/`
- Java 21 specific: `https://download.eclipse.org/justj/jres/21/updates/release/`

## Method 2: Manual JRE Inclusion via Features

The manual approach explicitly includes JRE-providing features in your product definition. This gives you more direct control over which JRE components are included.

### Configuration Example

**Step 1: Include JRE feature in your product file**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?pde version="3.5"?>

<product name="My Application" 
         uid="my.application.product" 
         version="1.0.0" 
         type="features" 
         includeLaunchers="true">
   
   <features>
      <!-- Include JustJ JRE feature explicitly -->
      <feature id="org.eclipse.justj.openjdk.hotspot.jre.full"/>
      
      <!-- Your application features -->
      <feature id="org.eclipse.platform" installMode="root"/>
      <!-- Your other features -->
   </features>
   
</product>
```

**Step 2: Add JustJ repository to your pom.xml**

```xml
<repositories>
  <repository>
    <id>justj</id>
    <url>https://download.eclipse.org/justj/jres/21/updates/release/</url>
    <layout>p2</layout>
  </repository>
</repositories>
```

**Step 3: Configure target platform**

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>target-platform-configuration</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <executionEnvironment>none</executionEnvironment>
  </configuration>
</plugin>
```

### When to Use Manual Inclusion

The manual approach is useful when:

- You want explicit control over which JRE feature variant is included
- You're migrating from an older product configuration
- You need to include additional JRE-related features or fragments

## Comparison of Methods

| Aspect | Automatic (`includeJRE="true"`) | Manual (Feature-based) |
|--------|--------------------------------|------------------------|
| **Simplicity** | Simple, minimal configuration | Requires explicit feature listing |
| **Flexibility** | Automatic resolution | Full control over JRE selection |
| **Maintenance** | Lower maintenance | Manual feature updates needed |
| **Recommended for** | Most new projects | Projects needing fine-grained control |

## Verifying JRE Inclusion

After building your product, verify that the JRE was included correctly:

1. **Check the build output**: Look for messages indicating JRE resolution
2. **Inspect the materialized product**: Navigate to `target/products/[product-id]/[os]/[ws]/[arch]/` and verify the JRE directory exists
3. **Test the product**: Launch the product and verify it runs without requiring a system-installed Java

The JRE is typically placed in a `jre` or `jdk` subdirectory within your product's root directory.

## Troubleshooting

### JRE Not Found During Build

**Problem**: Tycho cannot resolve the JRE during product materialization when using automatic inclusion.

**Solutions**:
- Ensure the `includeJRE="true"` attribute is present in your product file
- Check that the `productRepository` parameter is configured correctly (if customized)
- Verify internet connectivity to download from the JustJ repository

**Problem**: Tycho cannot resolve the JRE when using manual feature inclusion.

**Solutions**:
- Verify that your JustJ repository URL is correct and accessible in the `<repositories>` section
- Check that `executionEnvironment` is set to `none` in target-platform-configuration to avoid conflicts

### Multiple JRE Versions Resolved

**Problem**: Multiple JRE versions are being resolved, causing conflicts.

**Solutions**:
- When using automatic inclusion: Configure a specific JRE version via the `productRepository` parameter
- When using manual inclusion: Specify only one JustJ repository in your `<repositories>` section
- Review your target platform configuration to ensure no conflicting JRE sources

### Product Fails to Launch

**Problem**: The product builds successfully but fails to launch on the target system.

**Solutions**:
- Verify the JRE architecture (x86_64, aarch64) matches your product's target environment
- Check that the product's launcher is correctly configured to use the bundled JRE
- Ensure the JRE version is compatible with your Eclipse platform and plugins

### Understanding `executionEnvironment=none` (Manual Method Only)

**Why is this needed for manual JRE inclusion?**

When manually including JRE features in your product, you need to set `executionEnvironment=none` in the target-platform-configuration.

By default, Tycho injects mock "a.jre" units into the target platform to satisfy Java package imports (like `javax.xml`, `java.util`, etc.) and execution environment requirements. These mock units don't provide an actual JRE—they're just markers for dependency resolution.

When you explicitly add JustJ features to your target platform, which provides real JRE bundles with the same capabilities, you get conflicts. Setting `executionEnvironment=none` tells Tycho: "Don't inject your mock JRE units; I'm providing a real JRE through my target platform."

**Note**: This is NOT needed for automatic JRE inclusion with `includeJRE="true"`, as Tycho handles the JRE outside of the target platform.

## Complete Working Example

Here's a complete, minimal example for a product with automatic JRE inclusion:

**my-product.product:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?pde version="3.5"?>

<product name="My RCP Application" 
         uid="com.example.myapp" 
         id="com.example.myapp.product" 
         application="org.eclipse.ui.ide.workbench" 
         version="1.0.0.qualifier" 
         type="features" 
         includeLaunchers="true" 
         includeJRE="true"
         autoIncludeRequirements="true">

   <configIni use="default"/>
   
   <launcherArgs>
      <vmArgsMac>-XstartOnFirstThread</vmArgsMac>
   </launcherArgs>
   
   <launcher name="myapp">
      <win useIco="false">
         <bmp/>
      </win>
   </launcher>

   <features>
      <feature id="org.eclipse.platform" installMode="root"/>
      <!-- Add your application features here -->
   </features>

   <configurations>
      <plugin id="org.apache.felix.scr" autoStart="true" startLevel="2" />
      <plugin id="org.eclipse.core.runtime" autoStart="true" startLevel="0" />
      <plugin id="org.eclipse.equinox.common" autoStart="true" startLevel="2" />
   </configurations>

</product>
```

**pom.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  
  <groupId>com.example</groupId>
  <artifactId>com.example.myapp.product</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>eclipse-repository</packaging>
  
  <properties>
    <tycho-version>5.0.0</tycho-version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  
  <repositories>
    <repository>
      <id>eclipse-2024-12</id>
      <url>https://download.eclipse.org/releases/2024-12/</url>
      <layout>p2</layout>
    </repository>
  </repositories>
  
  <build>
    <plugins>
      <plugin>
        <groupId>org.eclipse.tycho</groupId>
        <artifactId>tycho-maven-plugin</artifactId>
        <version>${tycho-version}</version>
        <extensions>true</extensions>
      </plugin>
      
      <plugin>
        <groupId>org.eclipse.tycho</groupId>
        <artifactId>tycho-p2-director-plugin</artifactId>
        <version>${tycho-version}</version>
        <executions>
          <execution>
            <id>materialize-products</id>
            <goals>
              <goal>materialize-products</goal>
            </goals>
          </execution>
          <execution>
            <id>archive-products</id>
            <goals>
              <goal>archive-products</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

Build the product:
```bash
mvn clean verify
```

The resulting product archives will be in `target/products/` with the JRE included.

## Additional Resources

- [JustJ Project](https://www.eclipse.org/justj/) - Pre-packaged JRE distributions for Eclipse
- [Building Products](Products.html) - General information about building Eclipse products
- [Tycho P2 Director Plugin](tycho-p2-director-plugin/plugin-info.html) - Plugin for materializing products
- [Demo Projects](https://github.com/eclipse-tycho/tycho/tree/master/demo/justj) - Working examples of JRE inclusion
  - `automaticInstall` - Example using `includeJRE="true"`
  - `product` - Example using manual feature inclusion

## Common Questions

### Can I use a different JRE provider instead of JustJ?

Yes, you can use any JRE provider that publishes JRE artifacts as p2 installable units. The key requirement is that the JRE provider must supply p2 IUs with the appropriate capabilities.

### Does `includeJRE="true"` work with all operating systems?

Yes, Tycho automatically resolves the appropriate JRE for each target environment. JustJ provides JRE distributions for Windows, macOS, and Linux on various architectures (x86_64, aarch64).

### What's the difference between `jre.full` and other JRE features?

This question applies to the manual JRE inclusion method. JustJ provides different JRE feature variants:
- `org.eclipse.justj.openjdk.hotspot.jre.full` - Complete JRE with all modules
- `org.eclipse.justj.openjdk.hotspot.jre.minimal` - Minimal JRE for reduced size

Choose based on your application's Java module requirements. Most applications should use the `full` variant unless you have specific size constraints and know your exact module dependencies.

### How do I include JREs for multiple platforms?

Tycho automatically handles multi-platform builds. Simply define multiple target environments in your target-platform-configuration:

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>target-platform-configuration</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <environments>
      <environment>
        <os>win32</os>
        <ws>win32</ws>
        <arch>x86_64</arch>
      </environment>
      <environment>
        <os>linux</os>
        <ws>gtk</ws>
        <arch>x86_64</arch>
      </environment>
      <environment>
        <os>macosx</os>
        <ws>cocoa</ws>
        <arch>x86_64</arch>
      </environment>
    </environments>
  </configuration>
</plugin>
```

Tycho will automatically resolve and include the appropriate JRE for each platform when `includeJRE="true"` is set.
