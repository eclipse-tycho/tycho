#!/usr/bin/env python


# find ~/.m2/repository/ -name "*.pom" | grep -v "./org/eclipse/" | xargs -L1 -I{} parse_pom.py {}
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
	p = subprocess.call(["mvn", "help:effective-pom" , "-Doutput=/tmp/test.pom", "-f", sys.argv[1]], stdout=FNULL, stderr=FNULL)
	pom = xmltodict.parse(open("/tmp/test.pom"))
	project = pom["project"]
	groupId = project.get("groupId")
	artifactId=project.get("artifactId")
	version = project.get("version")

# TODO ignore packaging type pom
print("%s:%s:%s:%s" % (project["groupId"], project["artifactId"], project["version"], packaging))

