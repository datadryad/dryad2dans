package org.datadryad.dans;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.dansbagit.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.swordapp.client.*;

import javax.mail.MessagingException;
import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.SimpleFormatter;

public class DANSTransfer
{
    private static Logger log = Logger.getLogger(DANSTransfer.class);

    public static void main(String[] argv)
            throws Exception
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("i", "item", true, "item id (not handle) for datapackage item to transfer; supply this or -b or -a");
        options.addOption("b", "bag", true, "path to bag file to deposit; supply this or -i or -a");
        options.addOption("a", "all", false, "do all undeposited items; supply this or -i or -b");

        options.addOption("t", "temp", true, "local temp directory for assembling bags and zips");
        options.addOption("k", "keep", false, "specify this if you want the script to leave the zip file behind after exit");

        options.addOption("p", "package", false, "only package the content, do not deposit; supply this or -d");
        options.addOption("d", "deposit", false, "both package and deposit the content; supply this or -p");

        CommandLine line = parser.parse(options, argv);

        boolean all = line.hasOption("a");

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

        if (id == -1 && bagPath == null && !all)
        {
            System.out.println("You must specify either -i, -b or -a");
            DANSTransfer.printHelp(options);
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
        if (bagPath != null)
        {
            argCount++;
        }
        if (argCount > 1)
        {
            System.out.println("You may only specify one of -i, -b or -a");
            DANSTransfer.printHelp(options);
            System.exit(0);
        }

        boolean keep = line.hasOption("k");
        boolean dep = line.hasOption("d");
        boolean pack = line.hasOption("p");

        if (!(dep || pack))
        {
            System.out.println("You must specify one of -d or -p");
            DANSTransfer.printHelp(options);
            System.exit(0);
        }
        if (dep && pack)
        {
            System.out.println("You may only specify one of -d or -p");
            DANSTransfer.printHelp(options);
            System.exit(0);
        }

        if (!line.hasOption("t"))
        {
            System.out.println("You must specify a temporary working directory with -t");
            DANSTransfer.printHelp(options);
            System.exit(0);
        }

        DANSTransfer dt = new DANSTransfer(line.getOptionValue("t"), dep, keep);

        if (id > -1)
        {
            System.out.println("Item run requested");
            dt.doItem(id);
        }
        else if (bagPath != null)
        {
            System.out.println("Bag processing requested");
            dt.doBag(bagPath);
        }
        else if (all)
        {
            System.out.println("Process all requested");
            dt.doAllNew();
        }
    }

    public static void printHelp(Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DANSTransfer", options);
    }

    /** DSpace Context object */
    private Context context;

    private String tempDir = null;

    private String dansUsername = null;
    private String dansPassword = null;
    private String dansCollection = null;
    private String dansPackaging = null;
    private long maxChunkSize = -1;

    private boolean enableDeposit = true;
    private boolean keepZip = false;

    public DANSTransfer(String tempDir)
            throws Exception
    {
        this(tempDir, true, false);
    }

    public DANSTransfer(String tempDir, boolean enableDeposit, boolean keepZip)
            throws Exception
    {
        this(tempDir,
                ConfigurationManager.getProperty("dans", "dans.sword.username"),
                ConfigurationManager.getProperty("dans", "dans.sword.password"),
                ConfigurationManager.getProperty("dans", "dans.collection"),
                ConfigurationManager.getProperty("dans", "dans.packaging"),
                ConfigurationManager.getLongProperty("dans", "dans.max.chunk.size"),
                enableDeposit,
                keepZip);
    }

    public DANSTransfer(String tempDir, String username, String password, String collection, String packaging, long maxChunkSize, boolean enableDeposit, boolean keepZip)
            throws Exception
    {
        this.tempDir = tempDir;

        // FIXME: it's impossible to run this code outside of DSpace, so this is a hack to make this module
        // partially testable
        try {
            this.context = new Context();
            this.context.setIgnoreAuthorization(true);
        }
        catch (NoClassDefFoundError e) {
            System.out.println("Context failed to initialise");
        }

        this.dansUsername = username;
        this.dansPassword = password;
        this.dansCollection = collection;
        this.dansPackaging = packaging;
        this.maxChunkSize = maxChunkSize;

        this.enableDeposit = enableDeposit;
        this.keepZip = keepZip;
    }

    public void doAllNew()
            throws SQLException, Exception
    {
        log.info("Processing all items that have not previously been transferred");
        TransferIterator ii = TransferDAO.transferQueue(this.context);
        while (ii.hasNext()) {
            Item item = ii.next();
            this.doItem(item);
        }
    }

    public void doItem(int id)
            throws Exception
    {
        log.info("Processing item by id " + Integer.toString(id));
        Item datapackage = Item.find(this.context, id);
        this.doItem(datapackage);
    }

    public void doItem(Item datapackage)
            throws Exception
    {
        log.info("Processing item object with id " + Integer.toString(datapackage.getID()));
        DryadDataPackage ddp = new DryadDataPackage(datapackage);
        this.doDataPackage(ddp);
    }

    public void doDataPackage(DryadDataPackage ddp)
            throws Exception
    {
        DANSBag bag = null;
        log.info("Processing DryadDataPackage with id " + Integer.toString(ddp.getItem().getID()));
        try
        {
            bag = this.packageItem(ddp);
            if (this.enableDeposit)
            {
                log.info("Depositing DryadDataPackage with id " + Integer.toString(ddp.getItem().getID()));
                try
                {
                    DepositReceipt receipt = this.deposit(bag);
                    this.recordDeposit(ddp.getItem(), receipt);
                }
                catch (DANSTransferException e)
                {
                    this.recordError(ddp.getItem(), e);
                    this.sendErrorEmail(ddp.getItem(), e);
                }

            }
        }
        finally
        {
            if (bag != null)
            {
                log.info("Cleaning working directory " + bag.getWorkingDir());
                bag.cleanupWorkingDir();
                if (!this.keepZip)
                {
                    log.info("Cleaning zip file " + bag.getZipPath());
                    bag.cleanupZip();
                }
            }
            this.context.commit();
        }
    }

    public void doBag(String path)
    {
        // TODO
    }

    public DANSBag packageItem(DryadDataPackage ddp)
            throws Exception
    {
        Item item = ddp.getItem();
        log.info("Packaging item with id " + Integer.toString(item.getID()));

        String dirName = Integer.toString(item.getID());
        String workingDir = this.tempDir + File.separator + dirName;
        String zipPath = this.tempDir + File.separator + dirName + ".zip";

        String name = item.getHandle();
        DCValue[] idents = item.getMetadata("dc.identifier");
        if (idents.length > 0)
        {
            name = idents[0].value;
        }
        log.info("Packaging item " + Integer.toString(item.getID()) + " with the following parameters: name:" + name + "; zipPath:" + zipPath + "; workingDir:" + workingDir);

        DANSBag bag = new DANSBag(name, zipPath, workingDir);

        // put all of the item metadata in a DIM record
        DIMXWalk dimXwalk = new DIMXWalk();
        DIM dim = dimXwalk.makeDIM(item);
        bag.setDatasetDIM(dim);

        // put some item metadata in the DDM record
        DDMXWalk ddmXwalk = new DDMXWalk();
        DDM ddm = ddmXwalk.makeDDM(item);
        bag.setDDM(ddm);

        Set<DryadDataFile> ddfs = ddp.getDataFiles(this.context);
        log.info("Data package item " + Integer.toString(item.getID()) + " contains " + Integer.toString(ddfs.size()) + " Data Files");
        for (DryadDataFile ddf : ddfs)
        {
            Item df = ddf.getItem();
            log.info("Processing Data File item with id " + Integer.toString(df.getID()));

            String dfIdent = "";
            DCValue[] dfIdents = df.getMetadata("dc.identifier");
            if (dfIdents.length > 0)
            {
                dfIdent = dfIdents[0].value;
            }

            Bundle[] bundles = df.getBundles();
            for (Bundle bundle : bundles)
            {
                String bundleName = bundle.getName();

                Bitstream[] bitstreams = bundle.getBitstreams();
                for (Bitstream bitstream : bitstreams)
                {
                    InputStream is = BitstreamStorageManager.retrieve(this.context, bitstream.getID());
                    String bsName = bitstream.getName();
                    String format = bitstream.getFormat().getMIMEType();
                    String desc = bitstream.getDescription();

                    bag.addBitstream(is, bsName, format, desc, dfIdent, bundleName);
                }
            }

            DIM dfDim = dimXwalk.makeDIM(df);
            bag.addDatafileDIM(dfDim, dfIdent);
        }

        bag.writeToFile();
        log.info("Bag written to file " + bag.getZipPath());
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

                // FIXME: only the first deposit is done to the collection, the other deposits need to be done to the SE-IRI
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

    public void recordDeposit(Item item, DepositReceipt receipt)
            throws SQLException, AuthorizeException
    {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        String transferDate = sdf.format(now);

        String editIRI = receipt.getEditLink().getIRI().toString();

        String provenance = "Data Package successfully deposited to DANS at " + transferDate + ".  SWORDv2 identifier for item in DANS is " + editIRI;
        log.info(provenance);

        item.addMetadata("dryad", "dansTransferDate", null, null, transferDate);
        item.addMetadata("dryad", "dansEditIRI", null, null, editIRI);
        item.addMetadata("dc", "description", "provenance", null, provenance);
        item.update();
    }

    public void recordError(Item item, DANSTransferException e)
            throws SQLException, AuthorizeException
    {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        String transferDate = sdf.format(now);

        SWORDError se = (SWORDError) e.getCause();

        String provenance = "Data Package deposit to DANS failed at " + transferDate;
        log.error(provenance);

        item.addMetadata("dryad", "dansTransferFailed", null, null, transferDate);
        item.addMetadata("dc", "description", "provenance", null, provenance);
        item.update();
    }

    public void sendErrorEmail(Item item, DANSTransferException e)
            throws IOException, MessagingException
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String trace = sw.toString();

        String to = ConfigurationManager.getProperty("dans", "dans.errorEmail");

        Locale locale = I18nUtil.getDefaultLocale();
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(locale, "dans_deposit_error"));
        email.addArgument(trace);
        email.addArgument(Integer.toString(item.getID()));
        email.addRecipient(to);
        email.send();
    }
}
