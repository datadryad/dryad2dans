#!/bin/bash

# Script to transfer a single item from Dryad to DANS
#
# Usage dans-transfer.sh item-id

echo Transferring item to DANS...
set -e

sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -i $1 -d -t /opt/dryad-data/tmp/

echo

