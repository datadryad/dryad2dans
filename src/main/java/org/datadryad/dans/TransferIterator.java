package org.datadryad.dans;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;

import java.sql.SQLException;


/**
 * An Iterator in the style of the DSpace ItemIterator, for iterating over DSpace Items which are due to be
 * transferred to DANS.  Note that this class does not implement the Iterator interface, and does not extend the
 * ItemIterator either (it wraps it instead).
 *
 * This instance of the TransferIterator yields Items which DO NOT have the field dryad.dansTransferDate in their
 * metadata.  This means that all items which have previous transfer errors will be picked up again, and any items
 * which have been successfully deposited once will not be transferred again.
 *
 */
public class TransferIterator
{
    /** inner ItemIterator which this class uses as its basis */
    private ItemIterator itemIterator;

    /** The next item to be yielded */
    private Item nextItem = null;

    /**
     * Create an instance of the TransferIterator around a given ItemIterator
     * @param ii    A DSpace ItemIterator
     */
    public TransferIterator(ItemIterator ii)
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

    /**
     * Get the next item to be transferred.  If the internal field nextItem is populated this will return that value
     * and then unset it, otherwise, it will seek forward through the inner ItemIterator until it either reaches a new item
     * or reaches the end.  If an item is found it will be returned, otherwise null will be returned.
     *
     * @return  an item if there is a next one, or null if not
     * @throws SQLException
     */
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

    /**
     * Seek forward through the item iterator looking for the next item which meets the transfer criteria of having
     * the dryad.dansTransferDate populated.  If an item is found, it is set on the internal nextItem property of this
     * iterator and true is returned, otherwise this returns false
     *
     * @return  true of there is a next item, false if not
     * @throws SQLException
     */
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
