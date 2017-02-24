package org.datadryad.dans;

import org.dspace.content.Collection;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Data Access class for retrieving items from Dryad to be worked over by this module
 */
public class TransferDAO
{
    /**
     * Get a TransferIterator which will allow you to iterate over the items which are due to
     * be transferred to DANS.  See the documentation of the TransferIterator for details on
     * how that list is built
     *
     * @param context   the DSpace context
     * @return  a TransferIterator
     * @throws SQLException
     */
    public static TransferIterator transferQueue(Context context)
            throws SQLException
    {
        ItemIterator ii = TransferDAO.dataPackages(context);
        return new TransferIterator(ii);
    }

    public static MonitorIterator monitorQueue(Context context)
            throws SQLException
    {
        ItemIterator ii = TransferDAO.dataPackages(context);
        return new MonitorIterator(ii);
    }

    public static ItemIterator dataPackages(Context context)
        throws SQLException
    {
        Collection col = Collection.find(context, 2);
        return col.getAllItems();
    }
}
