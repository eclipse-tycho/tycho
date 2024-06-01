# Publish bundles and update-site to maven repository

This Demo shows how to publish a set of bundles as a P2 Update-Site to a maven repository (simulated by a folder here), 
with this approach it is possible to deploy a whole project consumable by P2 and maven users with only using the maven repository as the backing store.

As a result of building the project with `mvn clean deploy -DaltDeploymentRepository=snapshot-repo::default::file:maven-repository` you can inspect the following items:

- find the produced site in `site/target/repository`
- see the deployed artifacts in the file base maven repository under `maven-repository`
- such a repository can be used in Eclipse for example by adding the following update-site with `Help` > `Install New Software`: `mvn:org.eclipse.tycho.demo:p2-maven-site:1.0.0:zip:p2site`