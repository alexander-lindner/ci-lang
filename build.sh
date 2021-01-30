#!/bin/bash

if [[ "$GITHUB_REF" == refs/heads/* ]]; then
  export VERSIONING_GIT_BRANCH=${GITHUB_REF#refs/heads/}
elif [[ "$GITHUB_REF" == refs/tags/* ]]; then
  export VERSIONING_GIT_TAG=${GITHUB_REF#refs/tags/}
  echo "======================== setting version ========================"
  echo "======================== $VERSIONING_GIT_TAG ========================"
  ./mvnw -B versions:update-child-modules
  ./mvnw -B versions:set -DnewVersion="$VERSIONING_GIT_TAG" -DprocessAllModules
  ./mvnw -B versions:commit -DprocessAllModules
  ./mvnw -B -Dversioning.disable=true clean process-resources package
  echo "======================== setting version END ========================"
elif [[ "$GITHUB_REF" == refs/pull/*/merge ]]; then
  export VERSIONING_GIT_BRANCH=${GITHUB_REF#refs/}
  VERSIONING_GIT_BRANCH=${VERSIONING_GIT_BRANCH%/merge}
else
  ./mvnw -B clean process-resources package
fi

INTERPRETER="target/cish"

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
