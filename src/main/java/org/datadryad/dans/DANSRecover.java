package org.datadryad.dans;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.dansbagit.BaggedBitstream;
import org.datadryad.dansbagit.DANSBag;
import org.datadryad.dansbagit.DIM;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DANSRecover
{
    private static Logger log = Logger.getLogger(DANSRecover.class);

    public static void main(String[] argv)
            throws Exception
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("b", "bag", true, "path to bag file to import");
        options.addOption("t", "temp", true, "temporary working directory to use when unpacking the bag");
        options.addOption("d", "dryrun", false, "whether to do a dry-run import");

        CommandLine line = parser.parse(options, argv);

        if (!line.hasOption("b"))
        {
            System.out.println("You must specify a bag file to import with -b");
            DANSRecover.printHelp(options);
            System.exit(0);
        }

        if (!line.hasOption("t"))
        {
            System.out.println("You must specify a temporary directory with -t");
            DANSRecover.printHelp(options);
            System.exit(0);
        }

        boolean dryrun = line.hasOption("d");
        String bagFile = line.getOptionValue("b");
        String tempDir = line.getOptionValue("t");

        System.out.println("Importing bag " + bagFile + "using working directory " + tempDir);
        if (dryrun)
        {
            System.out.println("This will be a dry-run, no item will be imported into DSpace for real");
        }

        DANSRecover dr = new DANSRecover(tempDir, dryrun);
        dr.recover(bagFile);
    }

    public static void printHelp(Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DANSRecover", options);
    }

    private String tempDir;
    private boolean dryrun;
    private Context context;

    public DANSRecover(String tempDir, boolean dryrun)
            throws SQLException
    {
        this.tempDir = tempDir;
        this.dryrun = dryrun;

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

    public void recover(String bagPath)
            throws SQLException, IOException, AuthorizeException
    {
        log.info("Importing item to DSpace from " + bagPath);
        List<Bundle> removeBundles = new ArrayList<Bundle>();
        DANSBag db = null;
        try
        {
            db = new DANSBag(bagPath, this.tempDir);
            DIMXWalk dimXwalk = new DIMXWalk();

            DryadDataPackage dp = DryadDataPackage.create(this.context);
            log.info("Created new DryadDataPackage with id " + dp.getItem().getID());

            // populate the package with its metadata
            DIM dsDim = db.getDatasetDIM();
            dimXwalk.populateItem(dp.getItem(), dsDim);

            Set<String> idents = db.dataFileIdents();
            for (String ident : idents)
            {
                DryadDataFile df = DryadDataFile.create(this.context, dp);
                log.info("Created new DryadDataFile with id " + df.getItem().getID() + " for data file ident " + ident + " in DryadDataPackage with id " + dp.getItem().getID());

                DIM dfDim = db.getDatafileDIM(ident);
                dimXwalk.populateItem(df.getItem(), dfDim);

                Set<String> bundles = db.listBundles(ident);
                for (String bundle : bundles)
                {
                    Bundle dfBundle = df.getItem().createBundle(bundle);
                    log.info("Created bundle " + bundle + " in DryadDataFile with id " + df.getItem().getID());

                    Set<BaggedBitstream> bbs = db.listBitstreams(ident, bundle);
                    for (BaggedBitstream bb : bbs)
                    {
                        Bitstream bitstream = dfBundle.createBitstream(bb.getInputStream());
                        if (bb.getDescription() != null)
                        {
                            bitstream.setDescription(bb.getDescription());
                        }
                        if (bb.getFilename() != null)
                        {
                            bitstream.setName(bb.getFilename());
                        }
                        if (bb.getFormat() != null)
                        {
                            BitstreamFormat bsf = BitstreamFormat.findByMIMEType(this.context, bb.getFormat());
                            if (bsf != null)
                            {
                                bitstream.setFormat(bsf);
                            }
                        }
                        log.info("Created Bitstream with id " + bitstream.getID() + " in bundle " + bundle + " in DryadDataFile with id " + df.getItem().getID());
                    }

                    removeBundles.add(dfBundle);
                }

                // write the changes
                df.getItem().update();
            }

            // write the changes
            dp.getItem().update();
        }
        finally
        {
            if (db != null)
            {
                log.info("Cleaning up temporary directory");
                db.cleanupWorkingDir();
            }
            if (!this.dryrun)
            {
                log.info("Writing changes to DSpace");
                this.context.complete();
            }
            else
            {
                log.info("Dry-Run - reverting changes");
                for (Bundle bundle : removeBundles)
                {
                    Bitstream[] bitstreams = bundle.getBitstreams();
                    for (Bitstream bitstream : bitstreams)
                    {
                        bundle.removeBitstream(bitstream);
                    }
                }
                this.context.abort();
            }
        }
    }
}
