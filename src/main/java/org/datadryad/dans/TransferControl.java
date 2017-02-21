package org.datadryad.dans;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransferControl
{
    private static Logger log = Logger.getLogger(TransferControl.class);

    /**
     * Command line invocation target.  Run without arguments for help text
     *
     * @param argv
     * @throws Exception
     */
    public static void main(String[] argv)
            throws Exception
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("i", "item", true, "item id (not handle) for datapackage item to transfer; supply this or -a");
        options.addOption("a", "all", false, "do all items; supply this or -i");
        options.addOption("c", "clean", false, "request a clean of the metadata for the appropriate items");

        CommandLine line = parser.parse(options, argv);

        boolean all = line.hasOption("a");
        boolean clean = line.hasOption("c");

        int id = -1;
        if (line.hasOption("i"))
        {
            String idOpt = line.getOptionValue("i");
            id = Integer.parseInt(idOpt);
        }

        if (id == -1 && !all)
        {
            System.out.println("You must specify either -i or -a");
            TransferControl.printHelp(options);
            System.exit(0);
        }
        int argCount = 0;
        if (all)
        {
            argCount++;
        }
        if (id > -1)
        {
            argCount++;
        }
        if (argCount > 1)
        {
            System.out.println("You may only specify one of -i or -a");
            TransferControl.printHelp(options);
            System.exit(0);
        }

        TransferControl tc = new TransferControl();

        if (id > -1)
        {
            System.out.println("Item run requested");
            tc.cleanItem(id);
        }
        else if (all)
        {
            System.out.println("Process all requested");
            tc.cleanAll();
        }
    }

    public static void printHelp(Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("TransferControl", options);
    }

    /** DSpace Context object */
    private Context context;

    /**
     * Construct an instance of this class
     *
     * @throws SQLException if thrown by DSpace
     */
    public TransferControl()
            throws SQLException
    {
        // FIXME: it's impossible to run this code outside of DSpace, so this is a hack to make this module
        // partially testable
        try {
            this.context = new Context();
            this.context.setIgnoreAuthorization(true);
        }
        catch (NoClassDefFoundError e) {
            log.info("Context failed to initialised - you're probably testing outside DSpace");
        }
    }

    public void cleanAll()
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        log.info("Cleaning all items");
        ItemIterator ii = TransferDAO.dataPackages(this.context);
        while (ii.hasNext()) {
            Item item = ii.next();
            this.cleanItem(item);
        }
    }

    /**
     * Process a specific item by item ID.  ID should resolve to an item that is a Dryad Data Package
     *
     * param id     DSpace Item ID
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     * @throws MessagingException
     */
    public void cleanItem(int id)
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        log.info("Cleaning item by id " + Integer.toString(id));
        Item datapackage = Item.find(this.context, id);
        this.cleanItem(datapackage);
    }

    /**
     * Process a specific item.  Item should be a Dryad Data Package
     *
     * @param datapackage   DSpace Item
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     * @throws MessagingException
     */
    public void cleanItem(Item datapackage)
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        log.info("Cleaning item object with id " + Integer.toString(datapackage.getID()));
        DryadDataPackage ddp = new DryadDataPackage(datapackage);
        this.cleanDataPackage(ddp);
    }

    /**
     * Clean a sepcific DryadDataPackage
     *
     * @param ddp   the Dryad Data Package
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     * @throws MessagingException
     */
    public void cleanDataPackage(DryadDataPackage ddp)
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        log.info("Cleaning DryadDataPackage with id " + Integer.toString(ddp.getItem().getID()));

        Item item = ddp.getItem();

        DCValue[] editiri = item.getMetadata("dryad.dansEditIRI");
        DCValue[] tdate = item.getMetadata("dryad.dansTransferDate");
        DCValue[] fdate = item.getMetadata("dryad.dansTransferFailed");

        boolean mod = false;
        if (editiri != null && editiri.length > 0)
        {
            item.clearMetadata("dryad.dansEditIRI");
            mod = true;
        }
        if (tdate != null && tdate.length > 0)
        {
            item.clearMetadata("dryad.dansTransferDate");
            mod = true;
        }
        if (fdate != null && fdate.length > 0)
        {
            item.clearMetadata("dryad.dansTransferFailed");
            mod = true;
        }

        if (mod)
        {
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
            String cleanDate = sdf.format(now);

            String provenance = "Existing DANS metadata values cleared from this record on " + cleanDate;

            item.addMetadata("dc", "description", "provenance", null, provenance);
            item.update();
        }

        context.commit();

    }

}
