# Tycho Documentation

Eclipse Tycho is a set of Maven plugins and extensions for building Eclipse plug-ins, OSGi bundles, Eclipse features, update sites/p2 repositories, RCP applications, and BND workspaces with Maven.

## Getting Started

### [Tycho Build Extension](TychoBuildExtension.html)
The Tycho Build Extension is a core component that enables pomless builds, enhanced dependency resolution, and CI-friendly versioning. This is the starting point for enabling Tycho in your Maven projects.

### [Structured Build Layout and Pomless Builds](StructuredBuild.html)
Learn how to build Eclipse plugin projects without requiring a `pom.xml` file in every module. This simplifies project structure and reduces boilerplate configuration.

### [BND Workspace and Pomless Builds](BndBuild.html)
Build BND workspaces with Maven and Tycho, combining the power of BND tooling with Maven's dependency management.

## Core Concepts

### [Packaging Types](PackagingTypes.html)
Tycho defines custom Maven packaging types for Eclipse development including `eclipse-plugin`, `eclipse-feature`, `eclipse-repository`, and more. Understanding these is essential for working with Tycho.

### [Target Platform](TargetPlatform.html)
The target platform defines the set of bundles and features your project builds against. Learn how to configure and manage your target platform using p2 repositories, local installations, or `.target` files.

### [Testing Bundles](TestingBundles.html)
Run OSGi bundle tests with Tycho Surefire. This section covers unit testing, integration testing, and UI testing for Eclipse plugins.

## Building and Deployment

### [Building Products](Products.html)
Create Eclipse RCP applications and products with Tycho. Learn how to define products, materialize them, and create platform-specific distributions.

### [Signing Products](SignProducts.html)
Sign your Eclipse products for distribution using code signing certificates.

### [Creating update sites using category.xml](Category.html)
Build p2 repositories and Eclipse update sites for distributing your plugins and features.

### [Creating SBOMs for Eclipse Artifacts](SBOM.html)
Generate Software Bill of Materials (SBOM) documents for your Eclipse artifacts to track dependencies and improve security.

## Advanced Topics

### [Tycho CI Friendly Versions](TychoCiFriendly.html)
Use enhanced version properties for continuous integration workflows and automated releases.

### Properties
- [Build Properties](BuildProperties.html) - Configure build.properties for Eclipse projects
- [System Properties](SystemProperties.html) - Control Tycho behavior with system properties
- [Tycho Properties](TychoProperties.html) - Maven properties specific to Tycho

### [Troubleshooting](Troubleshooting.html)
Common issues and their solutions when working with Tycho.

## Plugin Reference

Tycho provides numerous Maven plugins for various aspects of Eclipse development:

### Main Plugins
- [Tycho Packaging Plugin](tycho-packaging-plugin/plugin-info.html) - Core packaging capabilities
- [Tycho Compiler Plugin](tycho-compiler-plugin/plugin-info.html) - Compile OSGi bundles with Eclipse JDT
- [Tycho Surefire Plugin](tycho-surefire-plugin/plugin-info.html) - Run tests in an OSGi runtime
- [Tycho P2 Repository Plugin](tycho-p2-repository-plugin/plugin-info.html) - Create p2 repositories
- [Tycho P2 Director Plugin](tycho-p2-director-plugin/plugin-info.html) - Assemble and materialize products

### Additional Plugins
- [Baseline Plugin](tycho-baseline-plugin/plugin-info.html) - Compare artifacts with baseline versions
- [BND Plugin](tycho-bnd-plugin/plugin-info.html) - Build BND workspaces
- [Declarative Services Plugin](tycho-ds-plugin/plugin-info.html) - Handle OSGi Declarative Services
- [PDE API Tools Plugin](tycho-apitools-plugin/plugin-info.html) - API analysis and compatibility checking
- [Source Plugin](tycho-source-plugin/plugin-info.html) - Package source bundles
- [Versions Plugin](tycho-versions-plugin/plugin-info.html) - Update project versions
- [GPG Signature Plugin](tycho-gpg-plugin/plugin-info.html) - Sign p2 artifacts with GPG
- [P2 Metadata Plugin](tycho-p2-plugin/plugin-info.html) - Generate p2 metadata
- [P2 Publisher Plugin](tycho-p2-publisher-plugin/plugin-info.html) - Publish artifacts to p2 repositories
- [CleanCode Plugin](tycho-cleancode-plugin/plugin-info.html) - Automatic code cleanup
- [Eclipse Plugin](tycho-eclipse-plugin/plugin-info.html) - Work with Eclipse installations
- [Wrap Plugin](tycho-wrap-plugin/plugin-info.html) - Wrap plain JARs into OSGi bundles
- [SBOM Plugin](tycho-sbom-plugin/plugin-info.html) - Generate Software Bill of Materials

### Tycho Extras
- [Document Bundle Plugin](tycho-extras/tycho-document-bundle-plugin/plugin-info.html) - Generate documentation bundles
- [P2 Extras Plugin](tycho-extras/tycho-p2-extras-plugin/plugin-info.html) - Additional p2 functionality
- [Target Platform Validation Plugin](tycho-extras/target-platform-validation-plugin/plugin-info.html) - Validate target platform definitions
- [Version Bump Plugin](tycho-extras/tycho-version-bump-plugin/update-target-mojo.html) - Update target platform versions

## Additional Resources

- [GitHub Repository](https://github.com/eclipse-tycho/tycho)
- [Issue Tracker](https://github.com/eclipse-tycho/tycho/issues)
- [Discussions](https://github.com/eclipse-tycho/tycho/discussions)
- [Mailing List](https://dev.eclipse.org/mailman/listinfo/tycho-dev)
