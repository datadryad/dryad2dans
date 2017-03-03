# Command Line Interface

## Transferring data to DANS

The command line script can be run with:

    ./dspace dsrun org.datadryad.dans.DANSTransfer
    
If you run this on its own it will output the help:

    usage: DANSTransfer
     -b,--bag       path to bag file to deposit; supply this or -i or -a
     -i,--item      item id (not handle) for datapackage item to transfer;
                    supply this or -b or -a
     -a,--all       do all undeposited items; supply this or -i or -b
     -p,--package   only package the content, do not deposit; supply this or
                    -d
     -d,--deposit   both package and deposit the content; supply this or -p
     -k,--keep      specify this if you want the script to leave the zip file
                    behind after exit
     -m,--monitor   monitor one or all items that need it and record any
                    success or fail states
     -t,--temp      local temp directory for assembling bags and zips


### Package Items for DANS (but do not transfer)

Use "-p" to only package the item.  Mostly this is useful for testing.

For example, the execute the transfer on an item with id 21, to restrict the operation to packaging only (no deposit),
and to keep the package around after, using /home/ubuntu/temp as the working directory, use the command:

    ./dspace dsrun org.datadryad.dans.DANSTransfer -i 21 -p -t /home/ubuntu/temp -k

To process all due deposits into packages, and keep the packages (don't do this in normal operation):

    ./dspace dsrun org.datadryad.dans.DANSTransfer -a -p -t /home/ubuntu/temp -k

### Transferring Items to DANS

Use "-d" to package and transfer the item to DANS.

To deposit a single item into the repository, cleaning up fully afterwards:

    ./dspace dsrun org.datadryad.dans.DANSTransfer -i 21 -d -t /home/ubuntu/temp
    
To deposit all due deposits into the repository, cleaning up fully afterwards (this will be the command you'll set as a cron job):

    ./dspace dsrun org.datadryad.dans.DANSTransfer -a -d -t /home/ubuntu/temp
    

### Monitoring Trasferred Items

Use "-m" to monitor previously trasferred items.

To monitor a single item in DANS, and update the item metadata if necessary, use:

    ./dspace dsrun org.datadryad.dans.DANSTransfer -i 21 -m

To monitor all pending items, and update thier metadata if necessary, use:

    ./dspace dsrun org.datadryad.dans.DANSTransfer -a -m

## Managing DANS Transfer Metadata

The command line script can be run with:

    ./dspace dsrun org.datadryad.dans.TransferControl
    
If you run this on its own it will output the help:

    usage: TransferControl
     -i,--item    item id (not handle) for datapackage item to transfer;
                  supply this or -a
     -a,--all     do all items; supply this or -i
     -c,--clean   request a clean of the metadata for the appropriate items
     
For example, to cleanup the DANS metadata from a single item, use:

    ./dspace dsrun org.datadryad.dans.TransferControl -i 21 -c
    
DO NOT DO THIS UNLESS YOU REALLY MEAN IT: To cleanup the DANS metadata from all items use:

    ./dspace dsrun org.datadryad.dans.TransferControl -a -c