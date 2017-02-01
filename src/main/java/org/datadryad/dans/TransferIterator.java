package org.datadryad.dans;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;

import java.sql.SQLException;

// an iterator, but does not implement Iterator, as ItemIterator (which it wraps) doesn't either

public class TransferIterator
{
    private ItemIterator itemIterator;

    private Item nextItem = null;

    public TransferIterator(ItemIterator ii)
    {
        this.itemIterator = ii;
    }

    public boolean hasNext()
            throws SQLException
    {
        if (!this.itemIterator.hasNext())
        {
            return false;
        }
        return this.populateNextItem();
    }

    public Item next()
            throws SQLException
    {
        if (this.nextItem == null)
        {
            this.populateNextItem();
        }
        Item item = this.nextItem;
        this.nextItem = null;
        return item;
    }

    private boolean populateNextItem()
            throws SQLException
    {
        while (this.itemIterator.hasNext())
        {
            Item item = this.itemIterator.next();
            DCValue[] dcvs = item.getMetadata("dryad.dansTransferDate");

            // if there is no transfer date, then this is the next item
            if (dcvs.length == 0)
            {
                this.nextItem = item;
                return true;
            }
        }
        return false;
    }
}
