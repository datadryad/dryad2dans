#!/bin/bash

# Script to transfer a single item from Dryad to DANS
#
# Usage dans-transfer.sh item-id

echo "Setting item as archived in DANS..."
set -e

sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -i $1 -a

echo

