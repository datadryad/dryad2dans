package org.datadryad.dans;

import org.datadryad.dansbagit.DDM;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Class which knows how to convert a DSpace Item's metadata to the DANS DDM format (and back again)
 */
public class DDMXWalk
{
    /** field mappings in the profile section of DDM */
    private static final Map<String, String> profile;
    static
    {
        profile = new HashMap<String, String>();
        profile.put("dc:title", "dc.title");
        profile.put("dc:description", "dc.description");
        profile.put("dc:creator", "dc.contributor.author");
        profile.put("ddm:created", "dc.date.issued");

        // what's missing from here?
        // profile.put("ddm:available", "????"); // when the record becomes available (e.g. end of embargo)
        // profile.put("ddm:audience", "????"); // narcis DisciplineType classification
        // profile.put("ddm:accessRights", "????") // one of a controlled list of allowed access rights terms (see ddm.xsd)
    }

    /** field mappings in the dcmi section of DDM */
    private static final Map<String, String> dcmi;
    static
    {
        dcmi = new HashMap<String, String>();
        dcmi.put("dcterms:identifier", "dc.identifier");
        dcmi.put("dcterms:hasPart", "dc.relation.haspart");
        dcmi.put("dcterms:isReferencedBy", "dc.relation.isreferencedby");
    }

    /** additional attributes to be applied for specific dcmi fields */
    private static final Map<String, Map<String, String>> dcmiAttrs;
    static
    {
        dcmiAttrs = new HashMap<String, Map<String, String>>();

        Map<String, String> identAttrs = new HashMap<String, String>();
        identAttrs.put("xsi:type", "id-type:DOI");
        dcmiAttrs.put("dcterms:identifier", identAttrs);
    }

    /**
     * Convert a DSpace Item's metadata into a DDM object for later serialisation
     *
     * @param item  The DSpace Item
     * @return  an instance of the DDM class which can be added to the DANSBag
     */
    public DDM makeDDM(Item item)
    {
        DDM ddm = new DDM();

        for (String profileField : DDMXWalk.profile.keySet())
        {
            String dsField = DDMXWalk.profile.get(profileField);
            DCValue[] dcvs = item.getMetadata(dsField);
            for (DCValue dcv : dcvs)
            {
                ddm.addProfileField(profileField, dcv.value);
            }
        }

        for (String dcmiField : DDMXWalk.dcmi.keySet())
        {
            String dsField = DDMXWalk.dcmi.get(dcmiField);
            DCValue[] dcvs = item.getMetadata(dsField);
            for (DCValue dcv : dcvs)
            {
                Map<String, String> attrs = null;
                if (dcmiAttrs.containsKey(dcmiField))
                {
                    attrs = dcmiAttrs.get(dcmiField);
                }
                ddm.addDCMIField(dcmiField, dcv.value, attrs);
            }
        }

        return ddm;
    }

    /**
     * Take a DDM object and use the data theirin to populate a DSpace Item
     * @param item
     * @param ddm
     */
    public void populateItem(Item item, DDM ddm)
    {
        // TODO
    }
}
