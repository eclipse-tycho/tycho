#!/usr/bin/env bash
# create the list of unapproved dependencies of tycho

cd "$(dirname $0)"
dependencies=$(./list_dependencies.sh)

for dep in $dependencies; do
  if ! grep -q "$dep" ./approved_dependencies.txt; then
    echo "No CQ found for dependency $dep"
  fi
done
