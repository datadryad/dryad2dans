
echo Transferring item to DANS...
set -e

while read item; do
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -i $item -d -t /transfer-complete/dans-transfer
done <items.txt

echo

