FROM ubuntu:20.10
MAINTAINER Alexander Lindner <25225552+alexander-lindner@users.noreply.github.com>
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update -q && apt-get install -qy git wget openjdk-15-jdk-headless maven && rm -rf /var/lib/apt/lists/*

ADD cish /bin/cish
RUN chmod +x /bin/cish

ENV HOME /data
ENV M2_HOME /usr/share/maven

WORKDIR /data
