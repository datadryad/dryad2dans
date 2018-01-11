package org.datadryad.dans;

import org.apache.log4j.Logger;
import org.datadryad.dansbagit.DIM;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Class which knows how to convert DSpace Item metadata into a DIM object (and back again)
 */
public class DIMXWalk
{
    private static Logger log = Logger.getLogger(DIMXWalk.class);

    /**
     * Convert a DSpace Item's metadata into the DIM object for later serialisation
     *
     * @param item  The DSpace Item
     * @return  an instance of the DIM object to be added to the DANSBag
     */
    public DIM makeDIM(Item item, VersionHistory history)
    {
	DCValue[] dansIDs = null;
	String dansEditIRI = null;
        DIM dim = new DIM();
        DCValue[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue dcv : dcvs)
        {
            dim.addField(dcv.schema, dcv.element, dcv.qualifier, dcv.value);
        }
        
	// add item's last modified date (since it isn't part of normal DSpace metadata)
	Date lastModified = item.getLastModified();
	SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSX");
        String lastModifiedString = sdf.format(lastModified);
	dim.addField("dc", "date", "lastModified", lastModifiedString);

	// add versioning information
        if(history != null) {
            DCValue[] idents = item.getMetadata("dc.identifier");
            String currentDOI = "";
            if (idents.length > 0) {
                currentDOI = idents[0].value;
            }
                 
            Version origVer = history.getFirstVersion();
            Item origItem = origVer.getItem();

	    dansIDs = origItem.getMetadata("dryad.dansEditIRI");
	    if (dansIDs.length > 0) {
		dansEditIRI = dansIDs[0].value;
	    }

            idents = origItem.getMetadata("dc.identifier");
            if (idents.length > 0) {
                String origDOI = idents[0].value;
                if(!currentDOI.equals(origDOI)) {
                    dim.addField("dc", "relation", "isversionof", origDOI);

                }
            }
	}

	// If a version of this item has been sent to DANS before, 
	// either this particular item, OR an earlier Dryad version,
	// add a dryad.DANSidentifier, so this will be marked as a new 
	// version in the DANS version chain. 
	dansIDs = item.getMetadata("dryad.dansEditIRI");
	if (dansIDs.length > 0) {
	    dansEditIRI = dansIDs[0].value;
	}
	if (dansEditIRI != null && dansEditIRI.length() > 0) {
	    // https://act.easy.dans.knaw.nl/sword2/container/7ad10055-bf08-4e5b-96ad-361f7277c957
	    int containerIndex = dansEditIRI.indexOf("/container/");
	    String dansID = dansEditIRI.substring(containerIndex + "/container/".length());
	    dim.addField("dryad", "DANSidentifier", null, dansID);	    
	}
        
        return dim;
    }

    /**
     * Populate a DSpace Item from the given DIM object from DANS
     *
     * @param item  The DSpace Item
     */
    public void populateItem(Item item, DIM dim)
    {
        for (String field : dim.listDSpaceFields())
        {
            for (String value : dim.getDSpaceFieldValues(field))
            {
                Map<String, String> fieldBits = dim.fieldBits(field);
                item.addMetadata(fieldBits.get("mdschema"), fieldBits.get("element"), fieldBits.get("qualifier"), null, value);
            }
        }
    }
}
