This document contains odds-and-ends of useful information, mostly in note form, to help in working with development
on this library.  It might not all be relevant for your particular environment.

# Server setup

On Ubuntu 14.04

    adduser dryad
    usermod -aG sudo dryad
    su - dryad

    sudo apt-get install virtualbox
    sudo apt-get install vagrant
    sudo apt-get install git
    sudo apt-get install unzip
    
    git clone https://github.com/ansible/ansible.git
    cd ansible
    git checkout a50d1ea
    
    cd ..
    git clone https://github.com/daisieh/vagrant-dryad.git
    cd vagrant-dryad/
    git checkout java8
    
    cp ansible-dryad/group_vars/all.template ansible-dryad/group_vars/all
    vim ansible-dryad/group_vars/all
    
Insert the following configuration values

    dryad:
        repo: git@github.com:CottageLabs/dryad-repo.git
        
    db:
        password: password
    testdb:
        password: password

Continue with the installation:

    cd
    wget https://releases.hashicorp.com/packer/0.11.0/packer_0.11.0_linux_amd64.zip
    unzip packer_0.11.0_linux_amd64.zip
    mkdir bin
    mv packer bin/
    source .profile
    
    cd vagrant-dryad/packer-templates/ubuntu-12.04/
    sh vagrant-box-dryad.sh
    

# Vagrant box layout

Dryad source (git):     /home/ubuntu/dryad-repo
Dryad installation:     /opt/dryad
Tomcat home:            /home/ubuntu/dryad-tomcat

1. Build dryad          /home/ubuntu/bin/build_dryad.sh
2. Deploy dryad         /home/ubuntu/bin/deploy_dryad.sh
3. Install database     /home/ubuntu/bin/install_dryad_database.sh
4. Start tomcat         /home/ubuntu/dryad-tomcat/bin/startup.sh
5. Rebuild SOLR indexes /home/ubuntu/bin/build_indexes.sh

Once Dryad is running, it can be updated and redeployed with:

/home/ubuntu/bin/redeploy_dryad.sh


# Outline of test data

**typical item**

1 10255/dryad.20 - package
2 10255/dryad.58
3 10255/dryad.23
4 10255/dryad.57

**the files in this package have README bitstreams in addition to the normal bitstreams**

5 10255/dryad.1295 - package
6 10255/dryad.1296
7 10255/dryad.1300

**the file in this package is under embargo until January 27th**

8 10255/dryad.106309 - package
9 10255/dryad.106310

**versioned item; most recent version**

10 10255/dryad.95765 - package
11 10255/dryad.95766
12 10255/dryad.95767

**versioned item; original version**

13 10255/dryad.37901 - package
14 10255/dryad.37902

**the single file in this package has a README bitstream in addition to the normal bitstream**

15 10255/dryad.7990 - package
16 10255/dryad.7991

**this item contains a larger bitstream; it is also in the “publication blackout” workflow state**

17 10255/dryad.134790 - package
18 10255/dryad.134791

# Importing test data

## Separate the packages and files into separate directories

In the test data directory:

    mkdir packages
    mkdir files
    cp -r 1 5 8 10 13 15 17 packages/
    cp -r 2 3 4 6 7 9 11 12 14 16 18 files/


## Import the items as usual with the DSpace item importer

In /opt/dryad/bin:

Data Packages should be imported to collection 2:

    ./dspace import -a -e richard@cottagelabs.com -c 2 -s /ubuntu/testdata/packages -m /ubuntu/mapfile_packages

Data Files should be imported to collection 1:

    ./dspace import -a -e richard@cottagelabs.com -c 1 -s /ubuntu/testdata/files -m /ubuntu/mapfile_files

## Manually insert the correct DOI into the doi database table and item metadata

Log into the database with:

    psql dryad_repo -U dryad_app

Find the data package items with:

    select * from item where owning_collection = 2;

Though the Dryad UI, when logged in, go to "Profile > Items" and search for the item ids from the above query.
  
For each item id, follow the link from the "Item Status" tab to the "Item Page".

On the first Data File on the Item Page, click on the link "View File Details" - this should produce an error.

In your browser select "View Source" from the context menu, and look at the full exception in the HTML comments.  You should see a line of the form:

    org.dspace.identifier.IdentifierNotFoundException: identifier doi:10.5061/dryad.xxxxx is not found

Insert this missing identifier into the doi table in the database with:

    insert into doi (doi_prefix, doi_suffix, url) values ('10.5061', 'dryad.xxxxxx', 'info:dspace/ITEM/[item id]');

Reload the Data File page in your browser to confirm this fixes the issue.

Back on the Item Page for the Data Package, select "Profile > Edit this Item" and go to the "Item Metadata" tab.  On this tab, look for the field dc.identifier
which will contain the incorrect DOI.  Replace this DOI with the one from above, and hit "Update" at the bottom of the page.

## Check for broken links

Review the doi table in the database, and ensure that all of the DOIs reference an item that was seen in the above process.  If not,
manually modify the Data Package to point to the correct DOI in the field dc.relation.haspart, and then re-do the steps above
for that Data Package.

# Logging in to the Dryad database

    psql dryad_repo -U dryad_app

# Example data packages

http://localhost:9999/handle/10255/dryad.20

http://localhost:9999/handle/10255/dryad.95765

http://localhost:9999/handle/10255/dryad.37901

http://localhost:9999/handle/10255/dryad.7990

http://localhost:9999/handle/10255/dryad.134790

http://localhost:9999/handle/10255/dryad.1295

http://localhost:9999/handle/10255/dryad.106309

# Easy SWORD2

https://github.com/DANS-KNAW/easy-sword2
https://github.com/DANS-KNAW/easy-sword2-dans-examples

# Tunnel from Vagrant machine to host machine

For example:

    vagrant ssh -- -R 12345:localhost:8080
    
Tunnel's the vagrant machine's port 12345 to the host machine's port 8080

If using this to tunnel out to easy-sword2 on the host machine, ensure you update dans.cfg to point to the correct port.