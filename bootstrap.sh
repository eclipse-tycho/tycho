#!/bin/bash

# Location of Eclipse SDK with RCP Delta pack
#TYCHO_TEST_TARGET_PLATFORM="-Dtycho.testTargetPlatform=/opt/eclipse-3.6-rcp/eclipse"

# location of maven used to build bootstrap tycho distribution
TYCHO_M2_HOME=/opt/maven

export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=128m"

export MAVEN_PARAMS="-Dmaven.repo.local=/tmp/tycho-bootstrap.localrepo -Dit.cliOptions=-U"

$TYCHO_M2_HOME/bin/mvn -f tycho-p2-resolver/pom.xml clean install -U -e -V ${MAVEN_PARAMS} || exit

$TYCHO_M2_HOME/bin/mvn clean install -U -e -V ${MAVEN_PARAMS} || exit

$TYCHO_M2_HOME/bin/mvn -f tycho-its/pom.xml clean test -U -e -V ${TYCHO_TEST_TARGET_PLATFORM} ${MAVEN_PARAMS} || exit

# uncomment to generate project documentation
# $TYCHO_M2_HOME/bin/mvn -Dsite.generation site:stage ${MAVEN_PARAMS} || exit
