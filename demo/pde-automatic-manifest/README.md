Build with automatic MANIFEST generation
========================================

This example shows how to combine projects that use the classical PDE project layout (MANIFEST first) with projects that use the automatic manifest generation, to build a feature based product using a pom-less setup.

It contains the following projects:

- tycho.demo.app - the application plugin project that contains the Application Model (classical PDE layout)
- tycho.demo.feature - the feature project that contains the custom plugins of this project to add to the product
- tycho.demo.inverter.ui - a plugin that contains some UI elements (classical PDE layout)
- tycho.demo.product - the product project to build the feature based product
- tycho.demo.service.api - a service API bundle (automatic manifest generation)
- tycho.demo.service.impl - a service implementation bundle (automatic manifest generation)
- tycho.demo.target - the target definition project that specifies the target platform to resolve the project dependencies