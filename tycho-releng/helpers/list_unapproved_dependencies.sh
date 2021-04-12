#!/usr/bin/env bash
# create the list of unapproved dependencies of tycho

cd "$(dirname $0)"
dependencies=$(./list_dependencies.sh)

found_unapproved="false"
for dep in $dependencies; do
  if ! grep -q "$dep" ./approved_dependencies.txt; then
    echo "No CQ found for dependency $dep"
    found_unapproved="true"
  fi
done
if [ "$found_unapproved" == "true" ]; then
  exit 1
fi
