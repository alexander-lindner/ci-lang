#!/bin/bash

if [[ "$GITHUB_REF" == refs/heads/* ]]; then
  export VERSIONING_GIT_BRANCH=${GITHUB_REF#refs/heads/}
elif [[ "$GITHUB_REF" == refs/tags/* ]]; then
  export VERSIONING_GIT_TAG=${GITHUB_REF#refs/tags/}
elif [[ "$GITHUB_REF" == refs/pull/*/merge ]]; then
  export VERSIONING_GIT_BRANCH=${GITHUB_REF#refs/}
  VERSIONING_GIT_BRANCH=${VERSIONING_GIT_BRANCH%/merge}
fi

echo $VERSIONING_GIT_TAG
echo ${GITHUB_REF##*/}
echo $GITHUB_REF

VERSION=$(./mvnw --non-recursive exec:exec -Dexec.executable='echo' -Dexec.args='${project.version}' -q)
echo "VERSION: $VERSION"
./mvnw versions:update-child-modules
./mvnw versions:set -DnewVersion="$VERSION" -DprocessAllModules
./mvnw versions:commit -DprocessAllModules

INTERPRETER="target/cish"
./mvnw -B clean process-resources package

if [ -f $INTERPRETER ]; then
  rm $INTERPRETER
fi

echo '#!/usr/bin/java -jar ' >$INTERPRETER
cat interpreter/target/interpreter-*-jar-with-dependencies.jar >>$INTERPRETER
chmod +x $INTERPRETER
cp $INTERPRETER docker/build

INTERPRETER="target/cish-alpine"
echo '#!/opt/openjdk-15/bin/java -jar ' >$INTERPRETER
cat interpreter/target/interpreter-*-jar-with-dependencies.jar >>$INTERPRETER
chmod +x $INTERPRETER
cp $INTERPRETER docker/build
