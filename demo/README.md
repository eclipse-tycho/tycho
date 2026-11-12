Tycho Demo Projects
===================

Sample projects demonstrating how to use Tycho.

* `itp01/`: Simple application that demonstrates how to build an eclipse plug-in and execute JUnit tests 
* `itp02/`: Simple application that demonstrates [use of pom-first dependencies](https://wiki.eclipse.org/Tycho/How_Tos/Dependency_on_pom-first_artifacts) 
* `itp04-rcp/`: [Trivial cross-platform RCP product application with root files which can be updated by means of p2](https://wiki.eclipse.org/Tycho/Demo_Projects/RCP_Application)  
* `testing`: Demonstrate different ways to test Bundles/Plugins with Tycho see https://tycho.eclipseprojects.io/doc/master/TestingBundles.html for more details.
* `pde-automatic-manifest`: Demonstrate the combination of classical PDE project layouts with project layouts that use automatic MANIFEST generation to build an Eclipse based product.
* `lombok`: Demonstrate how to use [Project Lombok](https://projectlombok.org/) with Tycho.
* `bnd-workspace/`: Build a BND Workspace with Maven without any poms.
* `bnd-pde-workspace/`: Build a mixture of BND Workspace and PDE projects with Maven.
* `custom-signing-product/`: Build a product repository with custom icons.
* `justj/`: Demonstrate how to include a JustJ JRE in a product, either manually (`product`) or with `includeJRE` (`automaticInstall`).
* `maven-dependency-embed/`: Embed a Maven dependency into a bundle using the `maven-dependency-plugin` (not common practice).
* `multi-release-jar/`: Build a [Multi-Release-Jar](https://openjdk.org/jeps/238) manifest-first with supplemental manifests.
* `multi-release-jar-classpath/`: Build a Multi-Release-Jar using the JDT `release` classpath attribute.
* `sbom-product/`: Create SBOMs for a product built for multiple platforms using the `tycho-sbom-plugin:generator` mojo.
* `osgi-repository/`: Publish bundles as an OSGi Repository to a Maven repository.
* `p2-maven-site/`: Publish bundles and a P2 update-site to a Maven repository.
* `publish-p2/`: Publish any bundle jar as a P2 update-site.
* `promote-p2/`: Promote a created repository to an Eclipse update site using the p2 manager.

Each demo project (except itp02) can be built by executing:

    mvn clean install

in the corresponding folder.

About Tycho
===========

  * [Project Homepage](https://github.com/eclipse-tycho/tycho)
  * [Documentation](https://www.eclipse.org/tycho/sitedocs/)
  * [Bug Tracker](https://github.com/eclipse-tycho/tycho/issues)
  * [How to Contribute](https://github.com/eclipse-tycho/tycho/blob/main/CONTRIBUTING.md)
  * [Contact Us](https://dev.eclipse.org/mailman/listinfo/tycho-user)

