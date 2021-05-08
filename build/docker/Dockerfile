FROM ubuntu:20.10
MAINTAINER Alexander Lindner <25225552+alexander-lindner@users.noreply.github.com>
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update -q && apt-get install -qy git wget openjdk-15-jdk-headless maven && rm -rf /var/lib/apt/lists/*

ENV HOME /app
ENV M2_HOME /usr/share/maven
WORKDIR /app

ADD cish /bin/cish
RUN mkdir -p /usr/lib/cish/dependencies
ADD lib/* /usr/lib/cish/dependencies/
RUN chmod +x /bin/cish