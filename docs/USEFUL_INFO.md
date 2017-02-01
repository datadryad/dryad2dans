# Importing test data

* Import the items as usual with the DSpace item importer

Data Packages should be imported to collection 2:

    ./dspace import -a -e richard@cottagelabs.com -c 2 -s /ubuntu/testdata/packages -m /ubuntu/mapfile1

Data Files should be imported to collection 1:

    ./dspace import -a -e richard@cottagelabs.com -c 1 -s /ubuntu/testdata/files -m /ubuntu/mapfile1_files

* Manually insert the correct DOI into the doi database table

* Go into the Data Package and check that the dc.identifier field matches the dc.relation.ispartof field in the Data Files

# Logging in to the Dryad database

psql dryad_repo -U dryad_app

# Example data packages

http://localhost:9999/handle/10255/dryad.20