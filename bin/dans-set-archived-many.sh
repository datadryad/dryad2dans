# Sets a list of items to have metadata indicating they are archived in DANS

# It is assumed that the list of items is stored in a 
# file in the current directory called items.txt,
# with one item ID per line.

echo "Setting items as archived in DANS..."

set -e

while read item; do
    # Perform the transfer
    sudo /opt/dryad/bin/dspace dsrun org.datadryad.dans.DANSTransfer -i $item -s
done <items.txt

echo

