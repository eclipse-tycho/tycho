# Building Products

Tycho supports building Eclipse RCP applications (products) through the `eclipse-repository` packaging type and the `tycho-p2-director-plugin`.

## Overview

Products are defined using `.product` files and can be materialized and packaged using Tycho's P2 Director Plugin. This enables you to create standalone applications based on Eclipse plugins and features.

## Basic Product Configuration

To build a product, create a project with the `eclipse-repository` packaging type that contains a `.product` file:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <artifactId>my.product</artifactId>
  <packaging>eclipse-repository</packaging>
  
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

## Profile Properties

Profile properties are a powerful feature that allows you to customize the behavior of the P2 installation profile when materializing products. These properties control various aspects of how the product is installed and configured.

### Common Profile Properties

#### Installing Sources Alongside Products

One of the most useful applications of profile properties is to include source bundles in your product installation. This is particularly helpful during development or when you want to enable debugging of the application.

Use the `installSources` parameter:

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-director-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <installSources>true</installSources>
  </configuration>
</plugin>
```

This is equivalent to setting the profile property `org.eclipse.update.install.sources=true`.

#### Installing Feature JARs

By default, feature JARs are installed with the product. You can control this behavior:

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-director-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <installFeatures>true</installFeatures>
  </configuration>
</plugin>
```

This is equivalent to setting the profile property `org.eclipse.update.install.features=true`.

### Custom Profile Properties

You can set arbitrary profile properties using the `profileProperties` configuration parameter:

```xml
<plugin>
  <groupId>org.eclipse.tycho</groupId>
  <artifactId>tycho-p2-director-plugin</artifactId>
  <version>${tycho-version}</version>
  <configuration>
    <profileProperties>
      <org.eclipse.update.install.sources>true</org.eclipse.update.install.sources>
      <org.eclipse.update.install.features>true</org.eclipse.update.install.features>
      <myCustomProperty>myValue</myCustomProperty>
    </profileProperties>
  </configuration>
</plugin>
```

## Complete Example

Here's a complete example that builds a product with sources included:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>my.product</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>eclipse-repository</packaging>

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
        <artifactId>tycho-p2-repository-plugin</artifactId>
        <version>${tycho-version}</version>
        <configuration>
          <includeAllDependencies>true</includeAllDependencies>
          <includeAllSources>true</includeAllSources>
        </configuration>
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
        <configuration>
          <products>
            <product>
              <id>my.product.id</id>
            </product>
          </products>
          <installSources>true</installSources>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

## Using Profiles for Development vs. Production

It's common to use Maven profiles to enable different configurations for development and production builds. For example, you might want to include sources only during development:

```xml
<profiles>
  <profile>
    <id>development</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.eclipse.tycho</groupId>
          <artifactId>tycho-p2-repository-plugin</artifactId>
          <version>${tycho-version}</version>
          <configuration>
            <includeAllSources>true</includeAllSources>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.eclipse.tycho</groupId>
          <artifactId>tycho-p2-director-plugin</artifactId>
          <version>${tycho-version}</version>
          <configuration>
            <installSources>true</installSources>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

Activate the profile with: `mvn clean verify -Pdevelopment`

## Additional Resources

- [Tycho P2 Director Plugin Documentation](tycho-p2-director-plugin/plugin-info.html) - Complete reference for all configuration options
- [Tycho P2 Repository Plugin Documentation](tycho-p2-repository-plugin/plugin-info.html) - For configuring the P2 repository
- [Packaging Types](PackagingTypes.html) - Information about the `eclipse-repository` packaging type
- [Signing Products](SignProducts.html) - How to sign custom Eclipse products with Tycho
