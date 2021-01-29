#!/bin/bash
INTERPRETER="target/cish"
mvn clean process-resources package

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
