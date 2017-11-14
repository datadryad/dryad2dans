# Install

To build and install this library onto an existing DSpace, there are a number of steps that need to be gone through.


## 1. Dependencies

First you need to have built and installed Dryad/DSpace in some suitable location.  You then need to install some DSpace code libraries
into your local maven repository in order to be able to compile this library:

    cd [dspace-source]/dspace/target/dspace-1.7.3-SNAPSHOT-build.dir/lib
        
    mvn install:install-file -Dfile=dspace-api-1.7.3-SNAPSHOT.jar -DgroupId=org.dspace -DartifactId=dspace-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar
    mvn install:install-file -Dfile=api-1.7.3-SNAPSHOT.jar -DgroupId=org.dspace.modules -DartifactId=api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar
    mvn install:install-file -Dfile=versioning-api-1.7.3-SNAPSHOT.jar -DgroupId=org.dspace.modules -DartifactId=versioning-api -Dversion=1.7.3-SNAPSHOT -Dpackaging=jar

You also need the DANS BagIt library installed, which can be got from here:

    git clone https://github.com/datadryad/dans-bagit.git
    cd dans-bagit
    mvn install

The JavaClient2.0 for SWORD requires a GPG key. If you do not already have one, you must generate one:

    sudo apt-get install rng-tools
    sudo rngd -r /dev/urandom
    gpg --gen-key
    (select all of the default options)

Then, install the JavaClient2.0 for SWORD:

    git clone https://github.com/swordapp/JavaClient2.0.git
    cd JavaClient2.0
    mvn install

(note, at time of writing we are attempting to put JavaClient2.0 into the Maven Central Repository, at which point this step will become unnecessary)


## 2. Build

You can download and build this library as follows:

    git clone https://github.com/datadryad/dryad2dans.git

then in the resulting directory:

    mvn clean package

You do not need to put this in your local maven repository, but if you'd like to you can just do:

    mvn install


## 3. Gather the dependencies

Once you have built the code itself, you also need to gather the dependencies that DSpace does not already provide.  You can 
do this with the following command:

    mvn dependency:copy-dependencies -DexcludeArtifactIds=junit,api,dspace-api,xom,xml-apis,xercesImpl,xalan,commons-codec,commons-httpclient,commons-logging,log4j,geronimo-stax-api_1.0_spec,wstx-asl,jaxen,lucene-core,icu4j,stax-api,xmlParserAPIs,rome,jdom,commons-cli,commons-lang,joda-time,servlet-api,commons-io


## 4. Deploy the code to DSpace

Copy the dependencies assembled in the previous step into the DSpace lib directory:

    cp target/dependency/* /opt/dryad/lib/

Copy the compiled code library into the DSpace lib directory

    cp target/dans-1.0-SNAPSHOT.jar /opt/dryad/lib


## 5. Deploy the configuration to DSpace

Copy over all the files in the /config directory of this module.  This includes

* config/emails/* - the email templates used by the module
* config/modules/dans.cfg - the configuration for the module

Once you have done this you must also modify config/modules/dans.cfg to your requirements (see inline documentation in that file)


## 6. Update DSpace metadata registry

You need to import the metadata fields used to control the DANS deposits into the DSpace metadata schema registry as follows (in [dspace]/bin):

    ./dspace registry-loader -dc [dryad2dans-source]/config/registries/dans-metadata.xml

Once you have done this, you need to restart tomcat for the changes to take effect.
