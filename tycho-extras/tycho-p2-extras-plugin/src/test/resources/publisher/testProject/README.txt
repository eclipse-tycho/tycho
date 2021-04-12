The project tycho-p2-extras-plugin/src/test/resources/testproject shows how one can use the 
features and bundles publisher.

Prerequisite tycho-p2-extras-plugin has been built successfully with 'mvn clean install'. 

Execute mvn clean install -Dtycho-version=<version> on 
tycho-p2-extras-plugin/src/test/resources/testProject so that the build will 

(1) download the bundle cxf-bundle-2.3.1.jar into testProject/target/source/plugins (==source to be 
    published), 
(2) publish the source with FeaturesAndBundlesPublisher into testProject/target/repository and 
(3) archive this repository as a zip file in directory testProject/target.

The testProject/pom.xml makes use of the following three build plugins and corresponding actions:

(1) Download a bundle (gav=org.apache.cxf/cxf-bundle/2.3.1) into directory 
    ${project.basedir}/target/source/plugins (build plugin with artifactId maven-dependency-plugin)

(2) Publishes the source ${project.basedir}/target/source by means of publish-features-and-bundles 
    goal to repository ${project.basedir}/target/repository (build plugin with artifactId 
    tycho-p2-extras-plugin).
    Note: The implementation of PublishFeatureAndBundlesMojo allows to configure some parameters 
    which are passed to the FeaturesAndBundlesPublisher. The configurable parameters are: 
    -metadataRepositoryLocation, -artifactRepositoryLocation, -sourceLocation, -compress, -append, 
    -publishArtifacts, -append, -reusePack200Files. As an example the testProject/pom.xml has 
    configured -compress as 'false' which lead to the published content content/artifacts.xml 
    instead of content/artifacts.jar. 
    Not configured parameters are filled with the default values specified in the 
    PublishFeatureAndBundlesMojo.java

(3) The results of step 2 - the published ${project.basedir}/target/repository contents 
   (content.xml, artifact.xml and /plugin/<artifact>.jar) are archived (zipped) into 
   ${project.basedir}/target/ (build plugin with artifactId tycho-p2-repository-plugin)
   Note: The archive-repository goal of the tycho-p2-repository-plugin works only with the 
   directory ${project.basedir}/target/repository (not if -metadataRepositoryLocation or  
   -artifactRepositoryLocation was configured pointing to other locations)







