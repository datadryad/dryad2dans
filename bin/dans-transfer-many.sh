# Transfers a list of items to DANS

# It is assumed that the list of items is stored in a 
# file in the current directory called items.txt,
# with one item ID per line.

echo Transferring items to DANS...

set -e

while read item; do
    # Clear metadata regarding previous DANS transfers, so there are no confusing edit IRIs
    # Note that provenance fields will not be cleared, so the transfer history is still human-readable
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.TransferControl -i $item -c

    # Perform the transfer
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -i $item -d -t /transfer-complete/dans-transfer
done <items.txt

echo

