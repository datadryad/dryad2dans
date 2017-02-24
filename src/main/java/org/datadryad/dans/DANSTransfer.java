package org.datadryad.dans;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.Resource;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Main class for controlling transfer of content from Dryad to DANS
 */
public class DANSTransfer
{
    private static Logger log = Logger.getLogger(DANSTransfer.class);

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

        options.addOption("i", "item", true, "item id (not handle) for datapackage item to transfer; supply this or -b or -a");
        options.addOption("b", "bag", true, "path to bag file to deposit; supply this or -i or -a");
        options.addOption("a", "all", false, "do all undeposited items; supply this or -i or -b");

        options.addOption("t", "temp", true, "local temp directory for assembling bags and zips");
        options.addOption("k", "keep", false, "specify this if you want the script to leave the zip file behind after exit");

        options.addOption("p", "package", false, "only package the content, do not deposit; supply this or -d");
        options.addOption("d", "deposit", false, "both package and deposit the content; supply this or -p");
        options.addOption("m", "monitor", false, "monitor one or all items that need it and record any success or fail states");

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
        boolean monitor = line.hasOption("m");

        if (!(dep || pack || monitor))
        {
            System.out.println("You must specify one of -d, -p or -m");
            DANSTransfer.printHelp(options);
            System.exit(0);
        }
        if (dep && pack || dep && monitor || pack && monitor)
        {
            System.out.println("You may only specify one of -d, -p or -m");
            DANSTransfer.printHelp(options);
            System.exit(0);
        }

        if ((dep || pack) && !line.hasOption("t"))
        {
            System.out.println("You must specify a temporary working directory with -t when using -d or -p");
            DANSTransfer.printHelp(options);
            System.exit(0);
        }

        DANSTransfer dt = new DANSTransfer(line.getOptionValue("t"), pack, dep, monitor, keep);

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
            if (dep)
            {
                dt.doAllNew();
            }
            else if (monitor)
            {
                dt.doAllMonitoring();
            }
        }
    }

    public static void printHelp(Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DANSTransfer", options);
    }

    /** DSpace Context object */
    private Context context;

    /** parent directory in which to create export working directories and zip files */
    private String tempDir = null;

    /** username to authenticate with DANS */
    private String dansUsername = null;

    /** password to authenticate with DANS */
    private String dansPassword = null;

    /** Target SWORDv2 deposit collection URI */
    private String dansCollection = null;

    /** packaging format to annotate zip files with */
    private String dansPackaging = null;

    /** Chunk size for continued deposits, in bytes */
    private long maxChunkSize = -1;

    private boolean pack = true;

    /** whether the class should actually carry out the deposit */
    private boolean enableDeposit = true;

    private boolean monitor = false;

    /** whether the created zip file should be retained after execution */
    private boolean keepZip = false;

    /**
     * Construct an instance of this class around the given temporary directory.  All other parameters will default
     * or be drawn from configuration.
     *
     * @param tempDir   directory in which all bagit working directories and zip files will be created
     * @throws SQLException if thrown by DSpace
     */
    public DANSTransfer(String tempDir)
        throws SQLException
    {
        this(tempDir, true, true, false, false);
    }

    /**
     * Construct an instance of this class around the given temporary directory, with deposit and keepZip set as required.
     * All other properties are set from configuration
     *
     * @param tempDir  directory in which all bagit working directories and zip files will be created
     * @param enableDeposit whether to carry out the deposit or not
     * @param keepZip   whether to keep the zip file after execution
     * @throws SQLException if thrown by DSpace
     */
    public DANSTransfer(String tempDir, boolean pack, boolean enableDeposit, boolean monitor, boolean keepZip)
            throws SQLException
    {
        this(tempDir,
                ConfigurationManager.getProperty("dans", "dans.sword.username"),
                ConfigurationManager.getProperty("dans", "dans.sword.password"),
                ConfigurationManager.getProperty("dans", "dans.collection"),
                ConfigurationManager.getProperty("dans", "dans.packaging"),
                ConfigurationManager.getLongProperty("dans", "dans.max.chunk.size"),
                pack,
                enableDeposit,
                monitor,
                keepZip);
    }

    /**
     * Construct an instance of this class around the given parameters.
     *
     * @param tempDir  directory in which all bagit working directories and zip files will be created
     * @param username  username to authenticate with DANS
     * @param password  password to authenticate with DANS
     * @param collection    collection URI to deposit to
     * @param packaging     package identifier to send along with deposit
     * @param maxChunkSize  chunk size for continued deposit, or -1 not to use continued deposit
     * @param enableDeposit whether to carry out the deposit or not
     * @param keepZip   whether to keep the zip file after execution
     * @throws SQLException if thrown by DSpace
     */
    public DANSTransfer(String tempDir, String username, String password, String collection, String packaging, long maxChunkSize, boolean pack, boolean enableDeposit, boolean monitor, boolean keepZip)
            throws SQLException
    {
        this.tempDir = tempDir;

        // FIXME: it's impossible to run this code outside of DSpace, so this is a hack to make this module
        // partially testable
        try {
            this.context = new Context();
            this.context.setIgnoreAuthorization(true);
        }
        catch (NoClassDefFoundError e) {
            log.info("Context failed to initialised - you're probably testing outside DSpace");
            // System.out.println("Context failed to initialise");
        }

        this.dansUsername = username;
        this.dansPassword = password;
        this.dansCollection = collection;
        this.dansPackaging = packaging;
        this.maxChunkSize = maxChunkSize;

        this.pack = pack;
        this.enableDeposit = enableDeposit;
        this.monitor = monitor;
        this.keepZip = keepZip;
    }

    /**
     * Process all the items in Dryad that need to be transferred to DANS.  See TransferDAO for details on how
     * the list of items is determined.
     *
     * @throws SQLException
     * @throws Exception
     */
    public void doAllNew()
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        log.info("Processing all items that have not previously been transferred");
        TransferIterator ii = TransferDAO.transferQueue(this.context);
        while (ii.hasNext()) {
            Item item = ii.next();
            this.doItem(item);
        }
    }

    public void doAllMonitoring()
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        log.info("Processing all items that have been transferred but not yet archived");
        MonitorIterator ii = TransferDAO.monitorQueue(this.context);
        while (ii.hasNext()) {
            Item item = ii.next();
            this.doItem(item);
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
    public void doItem(int id)
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        log.info("Processing item by id " + Integer.toString(id));
        Item datapackage = Item.find(this.context, id);
        this.doItem(datapackage);
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
    public void doItem(Item datapackage)
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        log.info("Processing item object with id " + Integer.toString(datapackage.getID()));
        DryadDataPackage ddp = new DryadDataPackage(datapackage);
        this.doDataPackage(ddp);
    }

    /**
     * Process a sepcific DryadDataPackage
     *
     * @param ddp   the Dryad Data Package
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     * @throws MessagingException
     */
    public void doDataPackage(DryadDataPackage ddp)
            throws IOException, SQLException, AuthorizeException, MessagingException
    {
        DANSBag bag = null;
        log.info("Processing DryadDataPackage with id " + Integer.toString(ddp.getItem().getID()));
        try
        {
            // if we've been asked to package OR deposit the item
            if (this.pack || this.enableDeposit)
            {
                log.info("Packaging DryadDataPackage with id " + Integer.toString(ddp.getItem().getID()));
                bag = this.packageItem(ddp);
            }

            // if we've been asked to deposit the item
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
                    this.recordDepositError(ddp.getItem(), e);
                    this.sendErrorEmail(ddp.getItem(), e);
                }
            }

            // if we've been asked to monitor the item
            if (this.monitor)
            {
                try
                {
                    this.updateStatus(ddp.getItem());
                }
                catch (DANSTransferException e)
                {
                    this.recordStateError(ddp.getItem(), null);
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

    /**
     * Process an individual bag file
     *
     * No implementation needed for this yet, may be removed
     *
     * @param path
     */
    public void doBag(String path)
    {
        // TODO
    }

    /**
     * Take a DryadDataPackage and generate a zip and corresponding DANSBag object, in preparation for deposit
     *
     * @param ddp   the DryadDataPackage to bag
     * @return  an instance of a DANSBag, where the zip file has already been written to disk for you using DANSBag.writeFile
     * @throws IOException
     * @throws SQLException
     */
    public DANSBag packageItem(DryadDataPackage ddp)
            throws IOException, SQLException
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

    /**
     * Deposit the bag represented by the DANSBag instance into DANS.
     *
     * Depending on the configuration of this DANSTransfer instance, this will deposit the entire zip as a single
     * request, or the chunks of the zip at suitable chunk sizes as a continued deposit.
     *
     * @param bag   DANSBag object, already with the zip written to disk
     * @return  the sword DepositReceipt
     * @throws DANSTransferException
     */
    public DepositReceipt deposit(DANSBag bag)
            throws DANSTransferException, IOException
    {
        // work out if we're doing this in one hit, or as a continued deposit
        if (bag.size() > this.maxChunkSize && this.maxChunkSize != -1)
        {
            // we're doing a continued deposit
            DepositReceipt receipt = null;
            FileSegmentIterator fsi = bag.getSegmentIterator(this.maxChunkSize, true);
            String seIRI = null;

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
                dep.setContentLength(fsis.getContentLength());

                AuthCredentials auth = new AuthCredentials(this.dansUsername, this.dansPassword);

                SWORDClient client = new SWORDClient();
                if (i == 1)
                {
                    try
                    {
                        receipt = client.deposit(this.dansCollection, dep, auth);
                        seIRI = receipt.getEditLink().getIRI().toString();
                        String stateIRI = receipt.getAtomStatementLink().getIRI().toString();
                        log.info("Got Edit IRI " + seIRI);
                        log.info("Got State IRI " + stateIRI);
                    }
                    catch (SWORDError e)
                    {
                        throw new DANSTransferException(e);
                    }
                    catch (SWORDClientException e)
                    {
                        throw new DANSTransferException(e);
                    }
                    catch (ProtocolViolationException e)
                    {
                        throw new DANSTransferException(e);
                    }
                }
                else
                {
                    if (seIRI == null)
                    {
                        throw new DANSTransferException("Initial deposit of continued deposit did not supply an SE-IRI; cannot continue");
                    }
                    try
                    {
                        DepositReceipt continued = client.addToContainer(seIRI, dep, auth);
                        log.info("Successfully deposited next segment");
                    }
                    catch (SWORDError e)
                    {
                        throw new DANSTransferException(e);
                    }
                    catch (SWORDClientException e)
                    {
                        throw new DANSTransferException(e);
                    }
                    catch (ProtocolViolationException e)
                    {
                        throw new DANSTransferException(e);
                    }
                }

                i++;
            }

            // return the original receipt
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
            dep.setContentLength(bag.size());

            AuthCredentials auth = new AuthCredentials(this.dansUsername, this.dansPassword);

            SWORDClient client = new SWORDClient();
            try
            {
                DepositReceipt receipt = client.deposit(this.dansCollection, dep, auth);
                String seIRI = receipt.getEditLink().getIRI().toString();
                String stateIRI = receipt.getAtomStatementLink().getIRI().toString();
                log.info("Got Edit IRI " + seIRI);
                log.info("Got State IRI " + stateIRI);
                return receipt;
            }
            catch (SWORDError e)
            {
                throw new DANSTransferException(e);
            }
            catch (SWORDClientException e)
            {
                throw new DANSTransferException(e);
            }
            catch (ProtocolViolationException e)
            {
                throw new DANSTransferException(e);
            }
        }
    }

    public void updateStatus(Item item)
            throws DANSTransferException, SQLException, AuthorizeException, MessagingException, IOException
    {
        DCValue[] dcvs = item.getMetadata("dryad.dansEditIRI");
        if (dcvs.length == 0)
        {
            log.info("Item with id " + Integer.toString(item.getID()) + " did not contain an Edit IRI - cannot update status");
            return;
        }

        String editIRI = dcvs[0].value;

        AuthCredentials auth = new AuthCredentials(this.dansUsername, this.dansPassword);
        SWORDClient client = new SWORDClient();
        try
        {
            DepositReceipt receipt = client.getDepositReceipt(editIRI, auth);
            Statement statement = client.getStatement(receipt, "application/atom+xml;type=feed", auth);
            if (statement == null)
            {
                String msg = "No Atom Statement was available in the DANS Deposit Receipt under " + editIRI;
                this.recordStateError(item, msg);
                this.sendErrorEmail(item, msg);
                return;
            }

            List<ResourceState> states = statement.getState();
            for (ResourceState state : states)
            {
                String term = state.getIri().toString();
                if ("INVALID".equals(term) || "REJECTED".equals(term) || "FAILED".equals(term))
                {
                    this.recordStateError(item, state.getDescription());
                    this.sendErrorEmail(item, state.getDescription());
                    return;
                }
                else if ("ARCHIVED".equals(term))
                {
                    this.recordStateArchived(item);
                    return;
                }
            }
        }
        catch (SWORDError e)
        {
            throw new DANSTransferException(e);
        }
        catch (SWORDClientException e)
        {
            throw new DANSTransferException(e);
        }
        catch (ProtocolViolationException e)
        {
            throw new DANSTransferException(e);
        }
        catch (StatementParseException e)
        {
            throw new DANSTransferException(e);
        }
    }

    /**
     * Record a successful deposit as represented by the DepositReceipt on the given DSpace Item
     *
     * This adds provenance metadata and datestamps for tracking.
     *
     * @param item  the DSpace Item successfully deposited.  Should be a Dryad Data Package
     * @param receipt   the SWORDv2 DepositReceipt for the successful deposit
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void recordDeposit(Item item, DepositReceipt receipt)
            throws SQLException, AuthorizeException
    {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        String transferDate = sdf.format(now);

        String editIRI = receipt.getEditLink().getIRI().toString();

        String provenance = "Data Package successfully transferred to DANS at " + transferDate + ".  SWORDv2 identifier for item in DANS is " + editIRI;
        log.info(provenance);

        item.addMetadata("dryad", "dansTransferDate", null, null, transferDate);
        item.addMetadata("dryad", "dansEditIRI", null, null, editIRI);
        item.addMetadata("dc", "description", "provenance", null, provenance);
        item.update();
    }

    /**
     * Record a failed deposit as represented by the DANS transfer exception
     *
     * @param item  The DSpace Item which failed to deposit
     * @param e the exception for the failure
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void recordDepositError(Item item, DANSTransferException e)
            throws SQLException, AuthorizeException
    {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        String transferDate = sdf.format(now);

        String provenance = "Data Package transfer to DANS failed at " + transferDate;
        log.error(provenance);

        item.addMetadata("dryad", "dansTransferFailed", null, null, transferDate);
        item.addMetadata("dc", "description", "provenance", null, provenance);
        item.update();
    }

    public void recordStateError(Item item, String msg)
            throws SQLException, AuthorizeException
    {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        String failedDate = sdf.format(now);

        String provenance = "Data Package processing by DANS recorded as failed at " + failedDate;
        if (msg != null)
        {
            provenance += " with message " + msg;
        }
        log.error(provenance);

        item.addMetadata("dryad", "dansProcessingFailed", null, null, failedDate);
        item.addMetadata("dc", "description", "provenance", null, provenance);
        item.update();
    }

    public void recordStateArchived(Item item)
            throws SQLException, AuthorizeException
    {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        String archiveDate = sdf.format(now);

        String provenance = "Data Package successfully archived by DANS at " + archiveDate;
        log.info(provenance);

        item.addMetadata("dryad", "dansArchiveDate", null, null, archiveDate);
        item.addMetadata("dc", "description", "provenance", null, provenance);
        item.update();
    }

    /**
     * Send an email to the Dryad administrator in the event of a deposit error
     *
     * @param item The DSpace Item which failed to deposit
     * @param e the exception for the failure
     * @throws IOException
     * @throws MessagingException
     */
    public void sendErrorEmail(Item item, DANSTransferException e)
            throws IOException, MessagingException
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String trace = sw.toString();

        this.sendErrorEmail(item, trace);
    }

    public void sendErrorEmail(Item item, String msg)
            throws IOException, MessagingException
    {
        String to = ConfigurationManager.getProperty("dans", "dans.errorEmail");

        if (to != null)
        {
            Locale locale = I18nUtil.getDefaultLocale();
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(locale, "dans_deposit_error"));
            email.addArgument(msg);
            email.addArgument(Integer.toString(item.getID()));
            email.addRecipient(to);
            email.send();
        }
    }
}
