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
public class MonitorIterator extends SelectiveIterator
{
    /**
     * Create an instance of the TransferIterator around a given ItemIterator
     * @param ii    A DSpace ItemIterator
     */
    public MonitorIterator(ItemIterator ii)
    {
        super(ii);
    }

    /**
     * Seek forward through the item iterator looking for the next item which meets the transfer criteria of having
     * the dryad.dansTransferDate populated.  If an item is found, it is set on the internal nextItem property of this
     * iterator and true is returned, otherwise this returns false
     *
     * @return  true of there is a next item, false if not
     * @throws SQLException
     */
    protected boolean populateNextItem()
            throws SQLException
    {
        while (this.itemIterator.hasNext())
        {
            Item item = this.itemIterator.next();
            DCValue[] tds = item.getMetadata("dryad.dansTransferDate");
            DCValue[] ads = item.getMetadata("dryad.dansArchiveDate");
            DCValue[] fds = item.getMetadata("dryad.dansProcessingFailed");

            if (tds.length > 0 && ads.length == 0 && fds.length == 0)
            {
                this.nextItem = item;
                return true;
            }
        }
        return false;
    }
}
