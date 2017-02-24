package org.datadryad.dans;

import org.dspace.content.Item;
import org.dspace.content.ItemIterator;

import java.sql.SQLException;

public abstract class SelectiveIterator
{
    /** inner ItemIterator which this class uses as its basis */
    protected ItemIterator itemIterator;

    /** The next item to be yielded */
    protected Item nextItem = null;

    /**
     * Create an instance of the TransferIterator around a given ItemIterator
     * @param ii    A DSpace ItemIterator
     */
    public SelectiveIterator(ItemIterator ii)
    {
        this.itemIterator = ii;
    }

    /**
     * Is there another Item to be transferred?  This method will seek forward through the inner ItemIterator
     * until it either reaches a new item, or reaches the end.  If an item is found, it will be stored in the private
     * field nextItem and true will be returned; otherwise returns false.
     *
     * @return  true if there is a next item, false if not
     * @throws SQLException
     */
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

    abstract boolean populateNextItem() throws SQLException;
}
