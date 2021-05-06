#!/bin/bash

# before the introduction of JPMS to cish, we was able to create an executable this way:
#
#echo '#!/usr/bin/java -jar ' >$INTERPRETER
#cat interpreter/target/interpreter-*-jar-with-dependencies.jar >>$INTERPRETER
#chmod +x $INTERPRETER
#cp $INTERPRETER docker/build

if [ -f /usr/bin/java ]; then
  javaPath="/usr/bin/java"
elif [ -f /opt/openjdk-15/bin/java ]; then
  javaPath="/usr/bin/java"
else
  javaPath="/usr/bin/env java"
fi

$javaPath --add-modules ALL-MODULE-PATH,ALL-SYSTEM -p /usr/lib/cish/dependencies/ -m org.alindner.cish.interpreter "$@"
