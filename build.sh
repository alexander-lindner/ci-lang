#!/bin/bash
INTERPRETER="target/cish"
mvn clean process-resources package

if [ -f $INTERPRETER ]; then
  rm $INTERPRETER
fi
#
echo '#!/usr/bin/java -jar ' >$INTERPRETER
cat interpreter/target/interpreter-0.1-SNAPSHOT-jar-with-dependencies.jar >>$INTERPRETER
chmod +x $INTERPRETER
cp $INTERPRETER docker/build
docker build -t alexanderlindner/cish:latest docker/build

#echo '#!/usr/bin/java --source 12 -cp target/ci-interpreter-0.1-SNAPSHOT-jar-with-dependencies.jar' >$INTERPRETER
#cat <<EOF >>$INTERPRETER
#import java.io.*;
#public class Wrapper {
#  public static void main(final String... args) throws IOException {
#    new org.alindner.cish.interpreter.Interpreter(args);
#  }
#}
#EOF
#chmod +x $INTERPRETER
