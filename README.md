# cish ![ci](https://github.com/alexander-lindner/ci-lang/workflows/ci/badge.svg) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Cish aims to provide a simpler and more human readable shell scripting language than bash. It is build CI first which means that not all necessary features which a "normal" lang
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

> ## :warning: The whole repo is in an experimental state. Maybe it doesn't get finished at all.

## Todos

* improve Readmes
* IO lib
* documentation
* more default libs
* function support (which isn't easy)
* github ci support
* FTP / SFTP / Webdav support
* SSH Support
* ENV lib
* git support
* zip / tar / ... support
* dpkg-build support
* Regex/sed support
* jar support
* parameter support
* curl & Json/XMl support
* OS tools like hostname, ip addr, ...

## Contributing

For development simply use maven. The project is configured for Intellij. For testing use the following line:
`sudo ln -s $(pwd)/target/cish /bin/cish`



