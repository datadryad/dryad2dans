package org.datadryad.dans;

import org.dspace.content.Collection;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

import java.sql.SQLException;

public class TransferDAO
{
    public static TransferIterator transferQueue(Context context)
            throws SQLException
    {
        Collection col = Collection.find(context, 2);
        ItemIterator ii = col.getAllItems();
        return new TransferIterator(ii);
    }
}
