#!/bin/bash

# Script to transfer a single item from Dryad to DANS
#
# Usage dans-transfer.sh item-id

echo Monitoring previously transferred items...
set -e

sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -m -a

echo

