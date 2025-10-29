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
- [Tycho Packaging Plugin](tycho-packaging-plugin/plugin-info.html) (`tycho-packaging-plugin`) - Core packaging capabilities
  - [package-plugin](tycho-packaging-plugin/package-plugin-mojo.html) - Packages OSGi bundles
  - [package-feature](tycho-packaging-plugin/package-feature-mojo.html) - Packages Eclipse features
  - [package-iu](tycho-packaging-plugin/package-iu-mojo.html) - Packages installable units
  - [package-target-definition](tycho-packaging-plugin/package-target-definition-mojo.html) - Packages target definitions
  - [build-qualifier](tycho-packaging-plugin/build-qualifier-mojo.html) - Generates build qualifier timestamps
  - [build-qualifier-aggregator](tycho-packaging-plugin/build-qualifier-aggregator-mojo.html) - Aggregates build qualifiers
  - [validate-id](tycho-packaging-plugin/validate-id-mojo.html) - Validates artifact IDs
  - [validate-version](tycho-packaging-plugin/validate-version-mojo.html) - Validates artifact versions
  - [update-consumer-pom](tycho-packaging-plugin/update-consumer-pom-mojo.html) - Updates consumer POMs
- [Tycho Compiler Plugin](tycho-compiler-plugin/plugin-info.html) (`tycho-compiler-plugin`) - Compile OSGi bundles with Eclipse JDT
  - [compile](tycho-compiler-plugin/compile-mojo.html) - Compiles main source code
  - [testCompile](tycho-compiler-plugin/testCompile-mojo.html) - Compiles test source code
  - [validate-classpath](tycho-compiler-plugin/validate-classpath-mojo.html) - Validates classpath
- [Tycho Surefire Plugin](tycho-surefire-plugin/plugin-info.html) (`tycho-surefire-plugin`) - Run tests in an OSGi runtime
  - [test](tycho-surefire-plugin/test-mojo.html) - Runs unit tests
  - [plugin-test](tycho-surefire-plugin/plugin-test-mojo.html) - Runs plugin tests
  - [verify](tycho-surefire-plugin/verify-mojo.html) - Verifies test results
- [Tycho P2 Repository Plugin](tycho-p2-repository-plugin/plugin-info.html) (`tycho-p2-repository-plugin`) - Create p2 repositories
  - [assemble-repository](tycho-p2-repository-plugin/assemble-repository-mojo.html) - Assembles p2 repository
  - [archive-repository](tycho-p2-repository-plugin/archive-repository-mojo.html) - Archives p2 repository
  - [assemble-maven-repository](tycho-p2-repository-plugin/assemble-maven-repository-mojo.html) - Assembles Maven repository
  - [fix-artifacts-metadata](tycho-p2-repository-plugin/fix-artifacts-metadata-mojo.html) - Fixes artifacts metadata
  - [verify-repository](tycho-p2-repository-plugin/verify-repository-mojo.html) - Verifies repository
- [Tycho P2 Director Plugin](tycho-p2-director-plugin/plugin-info.html) (`tycho-p2-director-plugin`) - Assemble and materialize products
  - [materialize-products](tycho-p2-director-plugin/materialize-products-mojo.html) - Materializes Eclipse products
  - [archive-products](tycho-p2-director-plugin/archive-products-mojo.html) - Archives Eclipse products
  - [director](tycho-p2-director-plugin/director-mojo.html) - Runs p2 director

### Additional Plugins
- [Baseline Plugin](tycho-baseline-plugin/plugin-info.html) (`tycho-baseline-plugin`) - Compare artifacts with baseline versions
  - [verify](tycho-baseline-plugin/verify-mojo.html) - Compares artifacts with baseline
  - [check-dependencies](tycho-baseline-plugin/check-dependencies-mojo.html) - Checks dependency versions
- [BND Plugin](tycho-bnd-plugin/plugin-info.html) (`tycho-bnd-plugin`) - Build BND workspaces
  - [build](tycho-bnd-plugin/build-mojo.html) - Builds BND workspace
  - [compile](tycho-bnd-plugin/compile-mojo.html) - Compiles BND projects
  - [test-compile](tycho-bnd-plugin/test-compile-mojo.html) - Compiles BND test code
  - [process](tycho-bnd-plugin/process-mojo.html) - Processes BND workspace
  - [integration-test](tycho-bnd-plugin/integration-test-mojo.html) - Runs integration tests
  - [initialize](tycho-bnd-plugin/initialize-mojo.html) - Initializes BND workspace
  - [clean](tycho-bnd-plugin/clean-mojo.html) - Cleans BND workspace
- [Declarative Services Plugin](tycho-ds-plugin/plugin-info.html) (`tycho-ds-plugin`) - Handle OSGi Declarative Services
  - [declarative-services](tycho-ds-plugin/declarative-services-mojo.html) - Generates DS component descriptors
- [PDE API Tools Plugin](tycho-apitools-plugin/plugin-info.html) (`tycho-apitools-plugin`) - API analysis and compatibility checking
  - [verify](tycho-apitools-plugin/verify-mojo.html) - Analyzes API compatibility
  - [generate](tycho-apitools-plugin/generate-mojo.html) - Generates API descriptions
- [Source Plugin](tycho-source-plugin/plugin-info.html) (`tycho-source-plugin`) - Package source bundles
  - [generate-pde-source-header](tycho-source-plugin/generate-pde-source-header-mojo.html) - Generates PDE source headers
- [Versions Plugin](tycho-versions-plugin/plugin-info.html) (`tycho-versions-plugin`) - Update project versions
  - [set-version](tycho-versions-plugin/set-version-mojo.html) - Sets project version
  - [set-property](tycho-versions-plugin/set-property-mojo.html) - Sets version property
  - [set-parent-version](tycho-versions-plugin/set-parent-version-mojo.html) - Sets parent version
  - [update-eclipse-metadata](tycho-versions-plugin/update-eclipse-metadata-mojo.html) - Updates Eclipse metadata
  - [update-pom](tycho-versions-plugin/update-pom-mojo.html) - Updates POM versions
- [GPG Signature Plugin](tycho-gpg-plugin/plugin-info.html) (`tycho-gpg-plugin`) - Sign p2 artifacts with GPG
  - [sign-p2-artifacts](tycho-gpg-plugin/sign-p2-artifacts-mojo.html) - Signs p2 artifacts with GPG
- [P2 Metadata Plugin](tycho-p2-plugin/plugin-info.html) (`tycho-p2-plugin`) - Generate p2 metadata
  - [p2-metadata](tycho-p2-plugin/p2-metadata-mojo.html) - Generates p2 metadata
  - [p2-metadata-default](tycho-p2-plugin/p2-metadata-default-mojo.html) - Generates default p2 metadata
  - [category-p2-metadata](tycho-p2-plugin/category-p2-metadata-mojo.html) - Generates category p2 metadata
  - [feature-p2-metadata](tycho-p2-plugin/feature-p2-metadata-mojo.html) - Generates feature p2 metadata
  - [update-site-p2-metadata](tycho-p2-plugin/update-site-p2-metadata-mojo.html) - Generates update site metadata
  - [update-local-index](tycho-p2-plugin/update-local-index-mojo.html) - Updates local index
  - [dependency-tree](tycho-p2-plugin/dependency-tree-mojo.html) - Displays dependency tree
- [P2 Publisher Plugin](tycho-p2-publisher-plugin/plugin-info.html) (`tycho-p2-publisher-plugin`) - Publish artifacts to p2 repositories
  - [publish-products](tycho-p2-publisher-plugin/publish-products-mojo.html) - Publishes products to p2
  - [publish-categories](tycho-p2-publisher-plugin/publish-categories-mojo.html) - Publishes categories to p2
  - [publish-osgi-ee](tycho-p2-publisher-plugin/publish-osgi-ee-mojo.html) - Publishes OSGi execution environments
  - [publish-ee-profile](tycho-p2-publisher-plugin/publish-ee-profile-mojo.html) - Publishes EE profiles
  - [attach-artifacts](tycho-p2-publisher-plugin/attach-artifacts-mojo.html) - Attaches artifacts
- [CleanCode Plugin](tycho-cleancode-plugin/plugin-info.html) (`tycho-cleancode-plugin`) - Automatic code cleanup
  - [cleanup](tycho-cleancode-plugin/cleanup-mojo.html) - Performs automatic code cleanup
  - [manifest](tycho-cleancode-plugin/manifest-mojo.html) - Cleans up manifest files
  - [quickfix](tycho-cleancode-plugin/quickfix-mojo.html) - Applies quick fixes
- [Eclipse Plugin](tycho-eclipse-plugin/plugin-info.html) (`tycho-eclipse-plugin`) - Work with Eclipse installations
  - [eclipse-run](tycho-eclipse-plugin/eclipse-run-mojo.html) - Runs Eclipse application
  - [eclipse-build](tycho-eclipse-plugin/eclipse-build-mojo.html) - Builds with Eclipse
- [Wrap Plugin](tycho-wrap-plugin/plugin-info.html) (`tycho-wrap-plugin`) - Wrap plain JARs into OSGi bundles
  - [wrap](tycho-wrap-plugin/wrap-mojo.html) - Wraps JARs as OSGi bundles
  - [verify](tycho-wrap-plugin/verify-mojo.html) - Verifies wrapped bundles
- [SBOM Plugin](tycho-sbom-plugin/plugin-info.html) (`tycho-sbom-plugin`) - Generate Software Bill of Materials
  - [generator](tycho-sbom-plugin/generator-mojo.html) - Generates SBOM document

### Tycho Extras
- [Document Bundle Plugin](tycho-extras/tycho-document-bundle-plugin/plugin-info.html) (`tycho-document-bundle-plugin`) - Generate documentation bundles
  - [build-help-index](tycho-extras/tycho-document-bundle-plugin/build-help-index-mojo.html) - Builds help index
  - [javadoc](tycho-extras/tycho-document-bundle-plugin/javadoc-mojo.html) - Generates Javadoc
  - [schema-to-html](tycho-extras/tycho-document-bundle-plugin/schema-to-html-mojo.html) - Converts schemas to HTML
  - [configure-document-bundle-plugin](tycho-extras/tycho-document-bundle-plugin/configure-document-bundle-plugin-mojo.html) - Configures document bundle plugin
- [P2 Extras Plugin](tycho-extras/tycho-p2-extras-plugin/plugin-info.html) (`tycho-p2-extras-plugin`) - Additional p2 functionality
  - [publish-features-and-bundles](tycho-extras/tycho-p2-extras-plugin/publish-features-and-bundles-mojo.html) - Publishes features and bundles
  - [mirror](tycho-extras/tycho-p2-extras-plugin/mirror-mojo.html) - Mirrors p2 repository
  - [compare-version-with-baselines](tycho-extras/tycho-p2-extras-plugin/compare-version-with-baselines-mojo.html) - Compares versions with baselines
- [Target Platform Validation Plugin](tycho-extras/target-platform-validation-plugin/plugin-info.html) (`target-platform-validation-plugin`) - Validate target platform definitions
  - [validate-target-platform](tycho-extras/target-platform-validation-plugin/validate-target-platform-mojo.html) - Validates target platform
- [Version Bump Plugin](tycho-extras/tycho-version-bump-plugin/plugin-info.html) (`tycho-version-bump-plugin`) - Update target platform versions
  - [update-target](tycho-extras/tycho-version-bump-plugin/update-target-mojo.html) - Updates target platform versions
  - [update-product](tycho-extras/tycho-version-bump-plugin/update-product-mojo.html) - Updates product versions
  - [update-manifest](tycho-extras/tycho-version-bump-plugin/update-manifest-mojo.html) - Updates manifest versions

## Additional Resources

- [GitHub Repository](https://github.com/eclipse-tycho/tycho)
- [Issue Tracker](https://github.com/eclipse-tycho/tycho/issues)
- [Discussions](https://github.com/eclipse-tycho/tycho/discussions)
- [Mailing List](https://dev.eclipse.org/mailman/listinfo/tycho-dev)
