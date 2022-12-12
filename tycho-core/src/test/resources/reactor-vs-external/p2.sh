#!/bin/sh

REPODIR=`cd extrepo; pwd`

jar cfm extrepo/plugins/org.sonatype.tycho.p2.impl.resolver.test.bundle01_2.0.0.jar \
 extrepo/org.sonatype.tycho.p2.impl.resolver.test.bundle01_2.0.0/META-INF/MANIFEST.MF


/opt/eclipse-3.6/eclipse/eclipse \
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

