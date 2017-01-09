package org.datadryad.dans.test;

import org.apache.commons.io.FileUtils;
import org.datadryad.dans.DANSTransfer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    public void testMakeBag()
            throws IOException, Exception
    {
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/working/testdans";
        this.cleanup.add(workingDir);

        String zipPath = System.getProperty("user.dir") + "/src/test/resources/working/testdans.zip";
        this.cleanup.add(zipPath);

        DANSTransfer dt = new DANSTransfer(workingDir);
    }
}
