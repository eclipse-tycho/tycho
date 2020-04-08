#!/usr/bin/env python

# script to list tycho test-only dependencies
# 1. clean local maven repo: rm -rf ~/.m2/repository
# 2. clone https://git.eclipse.org/c/tycho/org.eclipse.tycho.git/ and run 'mvn clean install'
# 3. clone https://git.eclipse.org/c/tycho/org.eclipse.tycho.extras.git/ and run 'mvn clean install'
# 4. cd org.eclipse.tycho/tycho-its and run 'mvn clean test'
# find ~/.m2/repository/ -name "*.pom" | grep -v "./org/eclipse/" | grep -iv tycho | sort | xargs -L1 -I{} parse_pom.py {}

import sys
import os
import xmltodict
import subprocess

pom = xmltodict.parse(open(sys.argv[1]))
project = pom["project"]
packaging=project.get("packaging")
if packaging is None:
    packaging="jar"
groupId = project.get("groupId")
artifactId = project.get("artifactId")
version = project.get("version")

if groupId is None or artifactId is None or version is None:
        FNULL = open(os.devnull, 'w')
        p = subprocess.call(["mvn", "help:effective-pom" , "-Doutput=/tmp/test.pom", "-Dmaven.repo.local=/tmp/mvn-repo/", "-f", sys.argv[1]], stdout=FNULL, stderr=FNULL)
        pom = xmltodict.parse(open("/tmp/test.pom"))
        project = pom["project"]
        groupId = project.get("groupId")
        artifactId=project.get("artifactId")
        version = project.get("version")

if packaging == "pom":
        sys.exit(0)
print("%s:%s:%s:%s" % (project["groupId"], project["artifactId"], project["version"], packaging))
