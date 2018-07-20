#!/bin/bash

# Script to monitor the status of a set of items at DANS
#
# It is assumed that the list of items is stored in a 
# file in the current directory called items.txt,
# with one item ID per line.

echo Monitoring previously transferred items...
set -e

while read item; do
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -m -i $item
done <items.txt

echo


