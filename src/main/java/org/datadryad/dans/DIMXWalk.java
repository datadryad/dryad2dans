package org.datadryad.dans;

import org.datadryad.dansbagit.DIM;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

public class DIMXWalk
{
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
}
