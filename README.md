# Dryad to DANS transfer library

## Introduction

TODO

## Build

In order to build this library you need to have already built
the Dryad instance of DSpace, so that you have the org.dspace:dspace-api:1.7.3-SNAPSHOT
artefact in your local maven repo.

You then also need to install the org.dspace.modules:api:1.7.3-SNAPSHOT artefact as follows:

    cd [dspace-source]/dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib
    
    mvn install:install-file -Dfile=api-1.7.3-SNAPSHOT.jar -DgroupId=org.dspace.modules -DartifactId=api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar

You can then download and build this library as follows:

    git clone https://github.com/datadryad/dryad2dans.git

then in the resulting directory:

    mvn clean package

to install into your local maven repository just use:

    mvn install