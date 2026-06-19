Promote Repository to Eclipse Update Site
=========================================

This example shows how to promote the created Eclipse repository using the p2 manager. For testing purposes, the update site is created locally and can be found under `promotion/target/updatesite`.

It contains the following projects:

- tycho.demo.plugin - the example plug-in project to be published to the update site
- tycho.demo.feature - the example feature  project to be published to the update site
- repository - responsible for creating the p2 repository
- promotion - responsible for promoting to the update site