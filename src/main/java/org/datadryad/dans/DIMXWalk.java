package org.datadryad.dans;

import org.datadryad.dansbagit.DIM;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

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
    public DIM makeDIM(Item item)
    {
        DIM dim = new DIM();
        DCValue[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue dcv : dcvs)
        {
            dim.addField(dcv.schema, dcv.element, dcv.qualifier, dcv.value);
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
        // TODO
    }
}
