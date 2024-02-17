#!/bin/bash -e
function usage
{
        echo "Please provide PGP key email"
        exit
}

[ "$#" -eq 1 ] || usage

CURRENT_SNAPSHOT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
RELEASE_VERSION=${CURRENT_SNAPSHOT_VERSION%-*}
echo "Performing Release $RELEASE_VERSION"
RELEASE_MAJOR=$(echo ${RELEASE_VERSION} | cut -d'.' -f1)
RELEASE_MINOR=$(echo ${RELEASE_VERSION} | cut -d'.' -f2)
RELEASE_MICRO=$(echo ${RELEASE_VERSION}| cut -d'.' -f3)
NEXT_RELEASE_MICRO=$((RELEASE_MICRO+1))
PREV_RELEASE_MICRO=$((RELEASE_MICRO-1))

PREV_RELEASE_VERSION=${RELEASE_MAJOR}'.'${RELEASE_MINOR}'.'${PREV_RELEASE_MICRO}
NEXT_RELEASE_VERSION=${RELEASE_MAJOR}'.'${RELEASE_MINOR}'.'${NEXT_RELEASE_MICRO}

waitforurl() {
  while [ $(curl -s -o /dev/null --head -w "%{http_code}" ${1}) -ne "200" ]; do
    printf '.'
    sleep 5
  done
  echo " Release record found!"
}

echo "Plase create a release record here:"
echo "- https://projects.eclipse.org/node/1631/create-release"
waitforurl https://projects.eclipse.org/projects/technology.tycho/releases/${RELEASE_VERSION}
git pull
git fetch --all --tags
mvn versions:set -DnewVersion=${RELEASE_VERSION}
sed -i -e "s|<version>${CURRENT_SNAPSHOT_VERSION}</version>|<version>${RELEASE_VERSION}</version>|g" tycho-extras/tycho-eclipserun-plugin/pom.xml
git add -u
git commit -m "Perform ${RELEASE_VERSION} release"
mvn clean deploy -Prelease -DskipTests \
     -DforgeReleaseId=sonatype-nexus-staging \
     -DforgeReleaseUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
     -Dgpg.keyname=$1 \
     -Dmaven.repo.local=/tmp/tycho-release
mvn install site site:stage -DskipTests -Dmaven.repo.local=/tmp/tycho-release
TYCHO_DIR=$(pwd)
pushd -n ${TYCHO_DIR}
cd ..
cd eclipse-tycho.github.io
git pull
cd doc
sed -i "11 a <li><a href='${RELEASE_VERSION}/index.html'>${RELEASE_VERSION}</a></li>" index.html
cp -r ${TYCHO_DIR}/target/staging ${RELEASE_VERSION}
rm latest
ln -s ${RELEASE_VERSION} latest
cd ..
git add -u
git add doc/${RELEASE_VERSION}
git commit -m "Add documentation for ${RELEASE_VERSION} release"
popd
git tag tycho-${RELEASE_VERSION}
mvn versions:set -DnewVersion=${NEXT_RELEASE_VERSION}-SNAPSHOT
sed -i -e "s|<version>${RELEASE_VERSION}</version>|<version>${NEXT_RELEASE_VERSION}-SNAPSHOT</version>|g" tycho-extras/tycho-eclipserun-plugin/pom.xml
git add -u
git commit -m "Prepare for ${NEXT_RELEASE_VERSION} release"
echo "Everything is prepared now, please review the release, the prepared docs and updated versions and close the staging repository:"
echo "- https://oss.sonatype.org/#stagingRepositories"
echo "After that, when all checks passed also release it!"
echo "Now wait until release became visible on maven central..."
waitforurl https://repo1.maven.org/maven2/org/eclipse/tycho/tycho-core/${RELEASE_VERSION}/tycho-core-${RELEASE_VERSION}.jar
echo "You should now push the branch and the documentation after that create the release record for Github here:"
echo "- https://github.com/eclipse-tycho/tycho/releases/new"
echo ""
echo "Subject: Tycho ${RELEASE_VERSION} is released"
echo ""
echo "Tycho ${RELEASE_VERSION} has been released and is available from Maven Central repository."
echo ""
echo "🆕 https://github.com/eclipse-tycho/tycho/blob/tycho-${RELEASE_VERSION}/RELEASE_NOTES.md"
echo "🏷️ https://github.com/eclipse-tycho/tycho/tree/tycho-${RELEASE_VERSION}"
echo "👔 https://projects.eclipse.org/projects/technology.tycho/releases/${RELEASE_VERSION}"
echo "🙏 contributors who contributed patches for this release:"
git log --pretty=format:%an tycho-${PREV_RELEASE_VERSION}..tycho-${RELEASE_VERSION} | sort | uniq
git log --grep="Also-[bB]y:" tycho-${PREV_RELEASE_VERSION}..tycho-${RELEASE_VERSION} | grep -i also-by | sed -e 's/.*Also-[bB]y:\s*\(.*\)/\1/' | sort | uniq
echo "💰 we would like to also thank <list of sponsors> for sponsoring contributions in this release"
echo ""
echo "and thanks to everyone who helped us with testing the snapshot version."
echo ""
echo "Regards,"
echo ""
echo " The Tycho Team"
echo ""
waitforurl https://github.com/eclipse-tycho/tycho/releases/tag/tycho-${RELEASE_VERSION}
echo "Now please send a note to the dev mailing list"
echo "- tycho-dev@eclipse.org"
