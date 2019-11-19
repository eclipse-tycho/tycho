#!/usr/bin/env bash
# create the list of compile dependencies of tycho
# this can be used when creating new CQs
# list of approved dependencies with corresponding CQs is in approved_dependencies.txt
# dependencies of scope test and pure test helpers like tycho-testing-harness are not considered
# test-only deps are all covered by workswith CQ https://dev.eclipse.org/ipzilla/show_bug.cgi?id=5252
cd "$(dirname $0)/../.."
mvn -B dependency:tree -pl '!tycho-testing-harness'| grep -E "\[INFO\] *[o|+\\].*:compile$" | sed -e "s/\[INFO\]\s*[+|\\ -]*//g" | grep -Ev "^org.eclipse.*:" |  sort | uniq
