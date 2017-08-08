#!/bin/bash

# Script to rebuild the Dryad2Dans package and install an updated version. This *only*
# works when Dryad2Dans has already been installed on the machine, using the standard 
# Dryad directory layout.

echo Rebuilding the DANS installation...
set -e

cd /home/ec2-user/dryad2dans
mvn clean package
mvn dependency:copy-dependencies -DexcludeArtifactIds=junit,api,dspace-api,xom,xml-apis,xercesImpl,xalan,commons-codec,commons-httpclient,commons-logging,log4j,geronimo-stax-api_1.0_spec,wstx-asl,jaxen,lucene-core,icu4j,stax-api,xmlParserAPIs,rome,jdom,commons-cli,commons-lang,joda-time,servlet-api,commons-io
sudo cp target/dependency/* /opt/dryad/lib/
sudo cp target/dans-1.0-SNAPSHOT.jar /opt/dryad/lib
sudo /opt/dryad/bin/dspace registry-loader -dc /home/ec2-user/dryad2dans/config/registries/dans-metadata.xml

echo
echo     ====  REBUILD SUCCEEDED ====
echo
