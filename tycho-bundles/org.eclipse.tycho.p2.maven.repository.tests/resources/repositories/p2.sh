#!/bin/sh

REPO=$1
REPODIR=`cd $REPO; pwd`

/opt/eclipse-3.5/eclipse/eclipse \
 -application org.eclipse.equinox.p2.metadata.generator.EclipseGenerator \
 -nosplash \
 -updateSite "$REPODIR" \
 -site "file://$REPODIR/site.xml" \
 -metadataRepository "file://$REPODIR" \
 -metadataRepositoryName "$REPO" \
 -artifactRepository "file://$REPODIR" \
 -artifactRepositoryName "$REPO" \
 -noDefaultIUs \
 -vmargs -Xmx512m

