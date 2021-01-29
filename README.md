# cish ![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/alexander-lindner/cish/ci/master?style=for-the-badge) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0) ![Docker Pulls](https://img.shields.io/docker/pulls/alexanderlindner/cish?style=for-the-badge) [![Maven Central](https://img.shields.io/maven-central/v/org.alindner.cish/compiler.svg?label=Maven%20Central&style=for-the-badge)](https://search.maven.org/search?q=g:%22org.alindner.cish%22%20AND%20a:%22compiler%22)

Cish aims to provide a simpler and more human-readable shell scripting language than bash. It is build CI first which means that not all necessary features which a "normal" shell
provide is build to cish.

```bash
#!/bin/cish -ldebug

#This script can be used to build cish 

INTERPRETER="target/cish"
Maven::clean()
Maven::pkg()

IO::removeIfExists(INTERPRETER)

IO::setContent(INTERPRETER, "#!/usr/bin/java -jar \\n")
IO::addContent(INTERPRETER, IO::getContent("interpreter/target/interpreter-0.1-SNAPSHOT-jar-with-dependencies.jar"))
IO::executable(INTERPRETER,true)

IO::copy(INTERPRETER,"docker/build")
Docker::build("docker/build","alexanderlindner/cish:latest")
```

Docker: https://hub.docker.com/r/alexanderlindner/cish

> ## :warning: The whole repo is in an experimental state. Maybe it doesn't get finished at all.

## Usage in your ci

### Gitlab CI

```yaml
image: alexanderlindner/cish:latest

demo-job-full:
  script:
    - chmod +x ./test.cish
    - ./test.cish

demo-job-lite:
  image: alexanderlindner/cish:alpine
  script:
    - chmod +x ./test.cish
    - ./test.cish

demo-job-tiny:
  image: alexanderlindner/cish:distroless
  script:
    - chmod +x ./test.cish
    - ./test.cish
```

### Manual

Using jq:

```bash
curl -L $(curl -s https://api.github.com/repos/alexander-lindner/cish/releases/latest | jq -r ".assets[] | select(.name | contains(\"cish\")) | .browser_download_url") > /bin/cish
chmod +x /bin/cish
```

Using plain bash:

```bash
curl -L $(curl -s https://api.github.com/repos/alexander-lindner/cish/releases/latest | grep "browser_download_url.*cish" | cut -d : -f 2,3 | tr -d \") > /bin/cish
chmod +x /bin/cish
```

## Contributing

For development simply use maven. The project is configured for Intellij. For inline developing, run the `org.alindner.cish.interpreter.Interpreter` class with a cish file as
parameter.

For testing of the current version use the following line:
`sudo ln -s $(pwd)/target/cish /bin/cish`



