#!/bin/bash

# Rebuilds and deploys the local installation of the Dryad2Dans module, for
# deploying newly updated code. It is assumed that the module has previously
# been installed, and all prerequisites are in place.

echo Rebuilding the DANS installation...
set -e

cd /home/ubuntu/dans-bagit
mvn install
cd /home/ubuntu/dryad2dans
mvn clean package
mvn dependency:copy-dependencies -DexcludeArtifactIds=junit,api,dspace-api,xom,xml-apis,xercesImpl,xalan,commons-codec,commons-httpclient,commons-logging,log4j,geronimo-stax-api_1.0_spec,wstx-asl,jaxen,lucene-core,icu4j,stax-api,xmlParserAPIs,rome,jdom,commons-cli,commons-lang,joda-time,servlet-api,commons-io
sudo cp target/dependency/* /opt/dryad/lib/
sudo cp target/dans-1.0-SNAPSHOT.jar /opt/dryad/lib

echo
echo     ====  REBUILD SUCCEEDED ====
echo
