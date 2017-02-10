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

    mvn clean package -DskipTests=true

to install into your local maven repository just use:

    mvn install -DskipTests=true
    
## Deployment in DSpace

At the moment this is just for developers, there is not a clean build pipeline.

### The code

You need to build the module with

    mvn clean package -DskipTests=true
    
You should also then gather the (new) DSpace dependencies with

    mvn dependency:copy-dependencies -DexcludeArtifactIds=junit,api,dspace-api,xom,xml-apis,xercesImpl,xalan,commons-codec,commons-httpclient,commons-logging,log4j,geronimo-stax-api_1.0_spec,wstx-asl,jaxen,lucene-core,icu4j,stax-api,xmlParserAPIs,rome,jdom,commons-cli,commons-lang,joda-time,servlet-api,commons-io
    
 Then copy them to the live Dryad directory
 
    cp target/dependency/* /opt/dryad/lib/
    
Finally, copy the main library over too

    cp target/dans-1.0-SNAPSHOT.jar /opt/dryad/lib

### The configuration

Make sure you copy over all the files in the /config directory of this module.  This includes

* config/emails/* - the email templates used by the module
* config/modules/dans.cfg - the configuration for the module

Once you have done this, you may need to restart tomcat for the changes to take effect.

## DSpace Setup

You need to import the DANS metadata into the DSpace metadata schema registry as follows (in [dspace]/bin):

    ./dspace registry-loader -dc [dryad2dans-source]/config/registries/dans-metadata.xml

Once you have done this, you may need to restart tomcat for the changes to take effect.

## Command Line

The command line script can be run with:

    ./dspace dsrun org.datadryad.dans.DANSTransfer
    
If you run this on its own it will output the help:

    usage: DANSTransfer
    -b,--bag       path to bag file to deposit; supply this or -i or -a
    -i,--item      item id (not handle) for datapackage item to transfer;
                 supply this or -b or -a
    -a,--all       do all undeposited items; supply this or -i or -b
    -p,--package   only package the content, do not deposit; supply this or
                 -d
    -d,--deposit   both package and deposit the content; supply this or -p
    -k,--keep      specify this if you want the script to leave the zip file
                 behind after exit
    -t,--temp      local temp directory for assembling bags and zips


For example, the execute the transfer on an item with id 21, to restrict the operation to packaging only (no deposit),
and to keep the package around after, using /home/ubuntu/temp as the working directory, use the command:

    ./dspace dsrun org.datadryad.dans.DANSTransfer -i 21 -p -t /home/ubuntu/temp -k

To process all due deposits into packages, and keep the packages (don't do this in normal operation):

    ./dspace dsrun org.datadryad.dans.DANSTransfer -a -p -t /home/ubuntu/temp -k
    

    ./dspace dsrun org.datadryad.dans.DANSTransfer -i 21 -d -t /home/ubuntu/temp