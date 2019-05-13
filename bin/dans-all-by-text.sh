#!/bin/bash

# Script to transfer all new Dryad content to DANS, based on text files instead of internal Java memory
# 
# Usage: dans-all-by-text.sh <temp-directory>

if [ $# -gt 0 ]; then
    DANS_TEMP=$1
else 
    DANS_TEMP="/transfer-complete/dans-transfer"
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

cd $DANS_TEMP
sudo /home/ubuntu/dryad2dans/bin/dans-make-item-list.sh > $DANS_TEMP/dansAll-${day}_${hour}.out & 
sleep 10
sudo /home/ubuntu/dryad2dans/bin/dans-transfer-many.sh >> $DANS_TEMP/dansAll-${day}_${hour}.out & 



