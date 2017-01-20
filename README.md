# Dryad to DANS transfer library

## Introduction

TODO

## Build

In order to build this library you need to have already built the Dryad instance of DSpace, so that you have the org.dspace:dspace-api:1.7.3-SNAPSHOT artefact in your local maven repo.

If you have a built version of DSpace elsewhere, and you just want to make sure this code will compile, you can do this manually with

    cd [dspace-source]/dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib
    
    mvn install:install-file -Dfile=dspace-api-1.7.3-SNAPSHOT.jar -DgroupId=org.dspace -DartifactId=dspace-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar

You then also need to install the org.dspace.modules:api:1.7.3-SNAPSHOT artefact as follows:

    cd [dspace-source]/dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib
    
    mvn install:install-file -Dfile=api-1.7.3-SNAPSHOT.jar -DgroupId=org.dspace.modules -DartifactId=api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar

Finally you also need the DANS BagIt library installed, which can be got from here:

    git clone https://github.com/datadryad/dans-bagit.git
    cd dans-bagit
    mvn install

You can then download and build this library as follows:

    git clone https://github.com/datadryad/dryad2dans.git

then in the resulting directory:

    mvn clean package

to install into your local maven repository just use:

    mvn install
    
## Deployment in DSpace

At the moment this is just for developers, there is not a clean build pipeline.

You need to build the module with

    mvn clean package
    
You should also then gather the (new) DSpace dependencies with

    mvn dependency:copy-dependencies -DexcludeArtifactIds=junit,api,dspace-api,xom,xml-apis,xercesImpl,xalan,commons-codec,commons-httpclient,commons-logging,log4j,geronimo-stax-api_1.0_spec,wstx-asl,jaxen,lucene-core,icu4j,stax-api,xmlParserAPIs,rome,jdom,commons-cli,commons-lang,joda-time,servlet-api,commons-io
    
 Then copy them to the live Dryad directory
 
    cp target/dependency/* /opt/dryad/lib/
    
Finally, copy the main library over too

    cp target/dans-1.0-SNAPSHOT.jar /opt/dryad/lib
