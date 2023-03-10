Build BND Workspace with PDE Demo
=================================

This example shows how one can build a a mixture of BND Workspace and PDE projects with Maven without just a single configurator pom.

It has the follwoing projects:

- cnf - the BND Workspace configured to use a eclipse JDT layout
- tycho.demo.api - an API bundle build by BND
- tycho.demo.util - a util bundle that is a PDE Plugin
- tycho.demo.impl - an implementation of the API that uses the util bundle and is build by BND