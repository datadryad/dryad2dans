#!/bin/bash

# Script to transfer a single item from Dryad to DANS
#
# Usage dans-transfer.sh item-id

echo Transferring item to DANS...
set -e

# Clear metadata regarding previous DANS transfers, so there are no confusing edit IRIs
# Note that provenance fields will not be cleared, so the transfer history is still human-readable
sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.TransferControl -i $1 -c

# Perform the transfer
sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -i $1 -p -k -t /transfer-complete/dans-transfer

echo

