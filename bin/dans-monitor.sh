#!/bin/bash

# Script to mointor a single item from Dryad to DANS transfer
#
# Usage dans-transfer.sh [item-id]
# If no item-id is transferred, will search the database for transferred
# items and monitor each of them.

echo Monitoring previously transferred items...
set -e

if [ $# -gt 0 ]; then
    # monitor one item by ID
    echo "Monitoring $1"
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -m -i $1
else 
    echo "Monitoring all items in the database"
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -m -a
fi


