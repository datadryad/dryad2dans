package org.datadryad.dans;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.datadryad.dansbagit.DANSBag;
import org.datadryad.dansbagit.DIM;
import org.datadryad.dansbagit.FileSegmentInputStream;
import org.datadryad.dansbagit.FileSegmentIterator;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.swordapp.client.*;

import java.io.File;
import java.io.InterruptedIOException;
import java.sql.SQLException;

public class DANSTransfer
{
    public static void main(String[] argv)
            throws Exception
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("i", "item", true, "item id (not handle) for dataset item to transfer");
        options.addOption("t", "temp", true, "local temp directory for assembling bags");
        options.addOption("b", "bag", true, "path to bag file to deposit");
        CommandLine line = parser.parse(options, argv);

        int id = -1;
        if (line.hasOption("i"))
        {
            String idOpt = line.getOptionValue("i");
            id = Integer.parseInt(idOpt);
        }

        String bagPath = null;
        if (line.hasOption("b"))
        {
            bagPath = line.getOptionValue("b");
        }

        DANSTransfer dt = new DANSTransfer(line.getOptionValue("t"));

        if (id > -1)
        {
            dt.doItem(id);
        }
        else if (bagPath != null)
        {
            dt.doBag(bagPath);
        }
    }

    /** DSpace Context object */
    private Context context;

    private String tempDir = null;

    private String dansUsername = null;
    private String dansPassword = null;
    private String dansCollection = null;
    private String dansPackaging = null;
    private long maxChunkSize = -1;

    public DANSTransfer(String tempDir)
            throws Exception
    {
        this(tempDir,
                ConfigurationManager.getProperty("dans", "dans.sword.username"),
                ConfigurationManager.getProperty("dans", "dans.sword.password"),
                ConfigurationManager.getProperty("dans", "dans.collection"),
                ConfigurationManager.getProperty("dans", "dans.packaging"),
                ConfigurationManager.getLongProperty("dans", "dans.max.chunk.size"));
    }

    public DANSTransfer(String tempDir, String username, String password, String collection, String packaging, long maxChunkSize)
            throws Exception
    {
        this.tempDir = tempDir;

        // FIXME: it's impossible to run this code outside of DSpace, so this is a hack to make this module
        // partially testable
        try {
            this.context = new Context();
        }
        catch (NoClassDefFoundError e) {
            System.out.println("Context failed to initialise");
        }

        this.dansUsername = username;
        this.dansPassword = password;
        this.dansCollection = collection;
        this.dansPackaging = packaging;
        this.maxChunkSize = maxChunkSize;

    }

    public void doTransfer()
            throws SQLException, Exception
    {
        ItemIterator ii = TransferDAO.transferQueue(this.context);
        while (ii.hasNext()) {
            Item item = ii.next();
            this.doItem(item);
        }
    }

    public void doItem(int id)
            throws Exception
    {
        Item dataset = Item.find(this.context, id);
        this.doItem(dataset);
    }

    public void doItem(Item dataset)
            throws Exception
    {
        DANSBag bag = this.packageItem(dataset);
        this.deposit(bag);
    }

    public void doBag(String path)
    {
        // TODO
    }

    public DANSBag packageItem(Item dataset)
            throws Exception
    {
        String dirName = Integer.toString(dataset.getID());
        String workingDir = this.tempDir + File.separator + dirName;
        String zipPath = this.tempDir + File.separator + dirName + ".zip";

        String name = dataset.getHandle();
        DCValue[] idents = dataset.getMetadata("dc.identifier");
        if (idents.length > 0)
        {
            name = idents[0].value;
        }

        DANSBag bag = new DANSBag(name, zipPath, workingDir);

        // put all of the item metadata in a DIM record
        DIM dim = new DIM();
        DCValue[] dcvs = dataset.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue dcv : dcvs)
        {
            dim.addField(dcv.schema, dcv.element, dcv.qualifier, dcv.value);
        }
        bag.setDatasetDIM(dim);

        // TODO
        // dereference the dc.relation.haspart items and copy in their bitstreams
        // appropriately

        bag.writeToFile();
        return bag;
    }

    public DepositReceipt deposit(DANSBag bag)
            throws Exception
    {
        // work out if we're doing this in one hit, or as a continued deposit
        if (bag.size() > this.maxChunkSize && this.maxChunkSize != -1)
        {
            // we're doing a continued deposit
            DepositReceipt receipt = null;
            FileSegmentIterator fsi = bag.getSegmentIterator(this.maxChunkSize, true);
            int i = 1;
            while (fsi.hasNext())
            {
                FileSegmentInputStream fsis = fsi.next();

                Deposit dep = new Deposit();
                dep.setPackaging(this.dansPackaging);
                dep.setMimeType("application/octet-stream");
                dep.setMd5(fsis.getMd5());
                dep.setFilename(bag.getZipName() + "." + Integer.toString(i));
                dep.setFile(fsis);
                dep.setInProgress(fsi.hasNext());

                AuthCredentials auth = new AuthCredentials(this.dansUsername, this.dansPassword);

                SWORDClient client = new SWORDClient();
                try
                {
                    receipt = client.deposit(this.dansCollection, dep, auth);
                }
                catch (SWORDError e)
                {
                    System.out.println(fsis.getMd5());
                    throw new DANSTransferException(e);
                }

                i++;
            }

            // return just the last receipt
            return receipt;
        }
        else
        {
            // ordinary one-hit deposit
            Deposit dep = new Deposit();
            dep.setPackaging(this.dansPackaging);
            dep.setMimeType("application/zip");
            dep.setMd5(bag.getMD5());
            dep.setFilename(bag.getZipName());
            dep.setFile(bag.getInputStream());

            AuthCredentials auth = new AuthCredentials(this.dansUsername, this.dansPassword);

            SWORDClient client = new SWORDClient();
            try
            {
                DepositReceipt receipt = client.deposit(this.dansCollection, dep, auth);
                return receipt;
            }
            catch (SWORDError e)
            {
                throw new DANSTransferException(e);
            }
        }
    }
}
