# Publish bundles as an OSGi Repository to maven repository

This Demo shows how to publish a set of bundles as an OSGi Repository and deploy it to a maven repository (simulated by a folder here), 
with this approach it is possible to deploy a whole project consumable by OSGi and maven users with only using the maven repository as the backing store.

This example uses a single bundle build with bnd-maven-plugin, but actually any jar project that produces a bundle (including Tycho ones) would work!

As a result of building the project with `mvn clean deploy -DaltDeploymentRepository=snapshot-repo::default::file:maven-repository` you can inspect the following items:

- find the produced site in `site/target/repository`
- see the deployed artifacts in the file base maven repository under `maven-repository`

**Currently there is no support in Eclipse/Tycho for consuming such repository!**