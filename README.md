# UNICORE Commandline Client (UCC)

This repository contains the source code for the
UNICORE Commandline Client (UCC), a full featured
client for UNICORE, written in Java.

## Download

The UCC can be downloaded from SourceForge
https://sourceforge.net/projects/unicore/files/Clients/Commandline%20Client/

Additionally, you will need a Java runtime (version 11 or later).

## Documentation

See the manual at
https://unicore-docs.readthedocs.io/en/latest/user-docs/ucc

## Building from source

You need Java and Apache Maven.

The Java code is built and unit tested using

    mvn install

To skip unit testing

    mvn install -DskipTests

The following commands create distribution packages:

    mvn install -DskipTests
    cd distribution
    # tgz
    mvn package -DskipTests -Ppackman -Dpackage.type=bin.tar.gz
    # deb
    mvn package -DskipTests -Ppackman -Dpackage.type=deb -Ddistribution=Debian
    # rpm
    mvn package -DskipTests -Ppackman -Dpackage.type=rpm -Ddistribution=RedHat
