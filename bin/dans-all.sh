#!/bin/bash

# Script to transfer all Dryad content to DANS

echo Transferring item to DANS...

sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer --all -d -t /opt/dryad-data/tmp/ > /opt/dryad-data/tmp/dansAll.out &

echo output being sent to /opt/dryad-data/tmp/dansAll.out

