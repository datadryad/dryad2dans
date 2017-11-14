package org.datadryad.dans;

import org.datadryad.dansbagit.DIM;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;

import java.util.List;
import java.util.Map;

/**
 * Class which knows how to convert DSpace Item metadata into a DIM object (and back again)
 */
public class DIMXWalk
{
    /**
     * Convert a DSpace Item's metadata into the DIM object for later serialisation
     *
     * @param item  The DSpace Item
     * @return  an instance of the DIM object to be added to the DANSBag
     */
    public DIM makeDIM(Item item, VersionHistory history)
    {
        DIM dim = new DIM();
        DCValue[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue dcv : dcvs)
        {
            dim.addField(dcv.schema, dcv.element, dcv.qualifier, dcv.value);
        }
        
        if(history != null) {
            DCValue[] idents = item.getMetadata("dc.identifier");
            String currentDOI = "";
            if (idents.length > 0) {
                currentDOI = idents[0].value;
            }
                 
            Version origVer = history.getFirstVersion();
            Item origItem = origVer.getItem();
            idents = origItem.getMetadata("dc.identifier");
            if (idents.length > 0) {
                String origDOI = idents[0].value;
                if(!currentDOI.equals(origDOI)) {
                    dim.addField("dc", "relation", "isversionof", origDOI);
                }
            }

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
