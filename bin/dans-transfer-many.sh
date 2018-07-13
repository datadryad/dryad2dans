
echo Transferring item to DANS...
set -e

while read item; do
    # Clear metadata regarding previous DANS transfers, so there are no confusing edit IRIs
    # Note that provenance fields will not be cleared, so the transfer history is still human-readable
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.TransferControl -i $item -c

    # Perform the transfer
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -i $item -d -t /transfer-complete/dans-transfer
done <items.txt

echo

