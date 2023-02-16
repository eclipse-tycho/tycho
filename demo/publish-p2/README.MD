# Publish jars as a P2 Update-Site

This Demo shows how to publish any jar (must be a bundle) as a P2 Update-Site, 
for this is uses the [maven-dependency-plugin](https://maven.apache.org/plugins/maven-dependency-plugin/copy-mojo.html) to copy a cxf bundle and pack it into a site.

The result of the build is as follows in the `target` folder:
- `source` - the copied jar used as a source
- `repository` - a folder containing the build P2 site, just upload it to a webserver to use it
- `published-p2-repository-1.0.0-SNAPSHOT.zip` - a packed version of the update-site, you might want to deploy it to a maven repository to use it together with the `mvn:<groupId>:<artifactId>:<version>:zip` protocol.