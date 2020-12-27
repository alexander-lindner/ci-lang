![ci](https://github.com/alexander-lindner/ci-lang/workflows/ci/badge.svg)

```bash
./build.sh
docker run -ti --rm --volume $(pwd):/build -w /build alexanderlindner/cish:latest bash
./build.cish


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
```