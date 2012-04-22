#!/bin/sh

REPO=$1
REPODIR=`cd $REPO; pwd`

/opt/eclipse-3.8/eclipse/eclipse -nosplash \
 -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
 -source $REPODIR \
 -metadataRepository "file://$REPODIR" \
 -artifactRepository "file://$REPODIR" \
 -publishArtifacts -reusePack200Files \
 -vmargs -Xmx512m

