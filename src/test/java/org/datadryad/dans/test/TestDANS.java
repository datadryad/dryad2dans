package org.datadryad.dans.test;

import org.apache.commons.io.FileUtils;
import org.datadryad.dans.DANSTransfer;
import org.datadryad.dansbagit.DANSBag;
import org.dspace.core.ConfigurationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.swordapp.client.DepositReceipt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestDANS
{
    private List<String> cleanup = new ArrayList<String>();

    @Before
    public void setUp()
    {
        this.cleanup = new ArrayList<String>();

        // FIXME: an attempt to load the test config from DSpace.  Currently unsuccessful
        String dspaceCfg = "/opt/dryad/config/dspace.cfg";
        ConfigurationManager.loadConfig(dspaceCfg);
    }

    @After
    public void tearDown()
            throws IOException
    {
        for (String path : this.cleanup)
        {
            File f = new File(path);
            if (!f.exists())
            {
                continue;
            }

            if (f.isDirectory())
            {
                FileUtils.deleteDirectory(f);
            }
            else
            {
                f.delete();
            }
        }
    }

    @Test
    public void testSendBag()
            throws IOException, Exception
    {
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/working/testdans";
        this.cleanup.add(workingDir);

        String zipPath = System.getProperty("user.dir") + "/src/test/resources/working/testdans.zip";

        DANSTransfer dt = new DANSTransfer(workingDir,
                "sword",
                "sword",
                "http://localhost:8080/easy-sword2/collection/1",
                "http://purl.org/net/sword/package/BagIt",
                -1,
                true,
                true);  // make sure we keep the bag, as this is a test resource
        DANSBag bag = new DANSBag("testbag", zipPath, workingDir);
        DepositReceipt receipt = dt.deposit(bag);
    }


    @Test
    public void testSendSegments()
            throws IOException, Exception
    {
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/working/testdans";
        this.cleanup.add(workingDir);

        String zipPath = System.getProperty("user.dir") + "/src/test/resources/working/testdans.zip";

        DANSTransfer dt = new DANSTransfer(workingDir,
                "sword",
                "sword",
                "http://localhost:8080/easy-sword2/collection/1",
                "http://purl.org/net/sword/package/BagIt",
                1000,
                true,
                true);  // make sure we keep the bag, as this is a test resource
        DANSBag bag = new DANSBag("testbag", zipPath, workingDir);
        DepositReceipt receipt = dt.deposit(bag);
    }

    @Test
    public void testSendReal()
            throws Exception
    {
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/working/testreal";
        this.cleanup.add(workingDir);

        String zipPath = System.getProperty("user.dir") + "/src/test/resources/bags/21.zip";

        DANSTransfer dt = new DANSTransfer(workingDir,
                "sword",
                "sword",
                "http://localhost:8080/easy-sword2/collection/1",
                "http://purl.org/net/sword/package/BagIt",
                -1,
                true,
                true);    // make sure we keep the bag, as this is a test resource
        DANSBag bag = new DANSBag("real", zipPath, workingDir);
        DepositReceipt receipt = dt.deposit(bag);
    }
}
