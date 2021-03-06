#!/bin/bash

if [[ "$GITHUB_REF" == refs/heads/* ]]; then
  export VERSIONING_GIT_BRANCH=${GITHUB_REF#refs/heads/}
elif [[ "$GITHUB_REF" == refs/tags/* ]]; then
  export VERSIONING_GIT_TAG=${GITHUB_REF#refs/tags/}
  echo "======================== setting version ========================"
  echo "======================== $VERSIONING_GIT_TAG ========================"
  rm -f .mvn/extensions.xml
  rm -f .mvn/maven-git-versioning-extension.xml
  ./mvnw -B versions:update-child-modules
  ./mvnw -B versions:set -DnewVersion="$VERSIONING_GIT_TAG" -DprocessAllModules
  ./mvnw -B versions:commit -DprocessAllModules
  echo "======================== setting version END ========================"
elif [[ "$GITHUB_REF" == refs/pull/*/merge ]]; then
  export VERSIONING_GIT_BRANCH=${GITHUB_REF#refs/}
  VERSIONING_GIT_BRANCH=${VERSIONING_GIT_BRANCH%/merge}
fi

INTERPRETER="./build/target/cish"
./mvnw -B -Dversioning.disable=true -DskipTests clean process-resources package
if [ -f $INTERPRETER ]; then
  rm $INTERPRETER
fi

rm -rf ./docker/build/lib
cp -r ./build/target/lib ./build/docker/
cp ./interpreter/target/interpreter-*.jar ./build/docker/lib/
cp ./build/execTemplate.sh ./build/docker/cish
cp ./build/execTemplate.sh $INTERPRETER
chmod +x $INTERPRETER
chmod +x ./build/docker/cish
