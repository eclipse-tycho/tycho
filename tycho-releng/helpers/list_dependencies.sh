#!/usr/bin/env bash
# create the list of compile dependencies of tycho
# this can be used when creating new CQs
# list of approved dependencies with corresponding CQs is in approved_dependencies.txt

cd "$(dirname $0)/../.."
mvn dependency:tree | grep -E "\[INFO\] *[o|+\\].*:compile$" | sed -e "s/\[INFO\]\s*[+|\\ -]*//g" | grep -v "^org.eclipse.tycho:" |  sort | uniq
