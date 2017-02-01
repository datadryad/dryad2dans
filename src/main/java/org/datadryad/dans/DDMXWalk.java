package org.datadryad.dans;

import org.datadryad.dansbagit.DDM;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

import java.util.HashMap;
import java.util.Map;

public class DDMXWalk
{
    private static final Map<String, String> profile;
    static
    {
        profile = new HashMap<String, String>();
        profile.put("dc:title", "dc.title");
        profile.put("dcterms:description", "dc.description");
        profile.put("dc:creator", "dc.contributor.author");
        profile.put("ddm:created", "dc.date.issued");
    }

    private static final Map<String, String> dcmi;
    static
    {
        dcmi = new HashMap<String, String>();
        dcmi.put("dcterms:identifier", "dc.identifier");
        dcmi.put("dcterms:hasPart", "dc.relation.haspart");
        dcmi.put("dcterms:isReferencedBy", "dc.relation.isreferencedby");
    }

    private static final Map<String, Map<String, String>> dcmiAttrs;
    static
    {
        dcmiAttrs = new HashMap<String, Map<String, String>>();

        Map<String, String> identAttrs = new HashMap<String, String>();
        identAttrs.put("xsi:type", "id-type:DOI");
        dcmiAttrs.put("dcterms:identifier", identAttrs);
    }

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
}
