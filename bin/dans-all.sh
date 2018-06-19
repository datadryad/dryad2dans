#!/bin/bash

# Script to transfer all new Dryad content to DANS
# 
# Usage: dans-all.sh <temp-directory>

if [ $# -gt 0 ]; then
    DANS_TEMP=$1
else 
    DANS_TEMP="/opt/dryad-data/tmp"
fi

LOCKFILE="/var/lock/.dans-export.exclusivelock"
day=`date "+%a"`
hour=`date "+%H"`

# ensure there is only one copy of this script running
lock() {
    exec 200>$LOCKFILE

    flock -n 200 \
        && return 0 \
        || return 1
}
lock || { exit 1; }

# do the transfer
echo "Transferring item to DANS using temp directory $DANS_TEMP"

sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer --all -d -t $DANS_TEMP/ > $DANS_TEMP/dansAll-${day}_${hour}.out & 



