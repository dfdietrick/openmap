// **********************************************************************
// <copyright>
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// </copyright>
// **********************************************************************
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/DetailRowMaker.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.Constants;
import com.bbn.openmap.layer.vpf.CoverageTable;
import com.bbn.openmap.layer.vpf.DcwColumnInfo;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.util.html.TableRowElement;

/**
 * A RowMaker class that will perform VDT (value description table)
 * lookups on selected columns in the table.
 */
public class DetailRowMaker extends PlainRowMaker {
    final HashMap intvdt;
    final HashMap charvdt;
    final DcwColumnInfo dcia[];

    /**
     * Constructor
     * 
     * @param drf the table being parsed
     * @param markupCols the column names of the columns to attempt
     *        lookups for
     */
    public DetailRowMaker(DcwRecordFile drf, String[] markupCols) {
        File pfile = new File(drf.getTableFile()).getParentFile();
        String tableName = drf.getTableName();
        intvdt = loadIntVDT(pfile, tableName);
        charvdt = loadCharVDT(pfile, tableName);
        DcwColumnInfo dc[] = drf.getColumnInfo();
        if (markupCols == null) {
            dcia = dc;
        } else {
            dcia = new DcwColumnInfo[dc.length];
            for (int i = 0; i < markupCols.length; i++) {
                int col = drf.whatColumn(markupCols[i]);
                if (col != -1) {
                    dcia[col] = dc[col];
                }
            }
        }
    }

    public void addToRow(TableRowElement row, List l) {
        int i = 0;
        for (Iterator vals = l.listIterator(); vals.hasNext();) {
            Object rval = vals.next();
            String vdt = (dcia[i] != null) ? dcia[i].getVDT() : null;
            if (vdt == null) {
                row.addElement(rval.toString());
            } else if (Constants.intVDTTableName.equals(vdt)
                    && (rval instanceof Number)) {
                int val = ((Number) rval).intValue();
                CoverageIntVdt civ = new CoverageIntVdt(dcia[i].getColumnName(), val);
                String lval = (String) intvdt.get(civ);
                row.addElement((lval == null) ? ("[" + val + "]") : lval);
            } else if (Constants.charVDTTableName.equals(vdt)
                    && (rval instanceof String)) {
                String val = (String) rval;
                CoverageCharVdt civ = new CoverageCharVdt(dcia[i].getColumnName(), val);
                String lval = (String) charvdt.get(civ);
                row.addElement((lval == null) ? ("[" + val + "]") : lval);
            } else {
                row.addElement("Table Data Error!");
            }
            i++;
        }
    }

    private HashMap loadIntVDT(File path, String tableName) {
        HashMap hm = new HashMap();
        try {
            File vdt = new File(path, Constants.intVDTTableName);
            if (vdt.canRead()) {
                DcwRecordFile intvdt = new DcwRecordFile(vdt.toString());
                int intcols[] = intvdt.lookupSchema(CoverageTable.VDTColumnNames,
                        true,
                        CoverageTable.intVDTschematype,
                        CoverageTable.intVDTschemalength,
                        false);

                List al = new ArrayList(intvdt.getColumnCount());
                while (intvdt.parseRow(al)) {
                    String tab = (String) al.get(intcols[0]);
                    if (!tableName.equalsIgnoreCase(tab)) {
                        continue;
                    }
                    String attr = (String) al.get(intcols[1]);
                    int val = ((Number) al.get(intcols[2])).intValue();
                    String desc = ((String) al.get(intcols[3])).intern();
                    hm.put(new CoverageIntVdt(attr, val), desc);
                }
                intvdt.close();
            }
        } catch (FormatException f) {
        }
        return hm;
    }

    private HashMap loadCharVDT(File path, String tableName) {
        HashMap hm = new HashMap();
        try {
            File vdt = new File(path, Constants.charVDTTableName);
            if (vdt.canRead()) {
                DcwRecordFile charvdt = new DcwRecordFile(vdt.toString());
                int charcols[] = charvdt.lookupSchema(CoverageTable.VDTColumnNames,
                        true,
                        CoverageTable.charVDTschematype,
                        CoverageTable.charVDTschemalength,
                        false);

                ArrayList al = new ArrayList(charvdt.getColumnCount());
                while (charvdt.parseRow(al)) {
                    String tab = (String) al.get(charcols[0]);
                    if (!tableName.equalsIgnoreCase(tab)) {
                        continue;
                    }
                    String attr = (String) al.get(charcols[1]);
                    String val = (String) al.get(charcols[2]);
                    String desc = ((String) al.get(charcols[3])).intern();
                    hm.put(new CoverageCharVdt(attr, val), desc);
                }
                charvdt.close();
            }
        } catch (FormatException f) {
        }
        return hm;
    }
}

/**
 * A utility class used to map information from a VPF feature table to
 * its associated value in an int.vdt file.
 */
class CoverageIntVdt {
    /**
     * the name of the attribute we are looking up (attribute is
     * interned)
     */
    final String attribute;
    /** the integer value we are looking up */
    final int value;

    /**
     * Construct a new object
     * 
     * @param a the value for the attribute member
     * @param v the value for the value member
     */
    public CoverageIntVdt(String a, int v) {
        attribute = a.toLowerCase().intern();
        value = v;
    }

    /**
     * Override the equals method. Two CoverageIntVdts are equal if
     * and only iff their respective attribute and value members are
     * equal.
     */
    public boolean equals(Object o) {
        if (o instanceof CoverageIntVdt) {
            CoverageIntVdt civ = (CoverageIntVdt) o;
            // we can use == rather than String.equals(String) since
            // attribute is interned.
            return ((attribute == civ.attribute) && (value == civ.value));
        } else {
            return false;
        }
    }

    /**
     * Override hashcode. Compute a hashcode based on our member
     * values, rather than our (base class) object identity.
     */
    public int hashCode() {
        return (attribute.hashCode() ^ value);
    }
}

/**
 * A utility class used to map information from a VPF feature table to
 * its associated value in an char.vdt file.
 */
class CoverageCharVdt {
    /**
     * the name of the attribute we are looking up (attribute is
     * interned)
     */
    final String attribute;
    /** the character value we are looking up (value is interned) */
    final String value;

    /**
     * Construct a new object
     * 
     * @param a the value for the attribute member
     * @param v the value for the value member
     */
    public CoverageCharVdt(String a, String v) {
        attribute = a.toLowerCase().intern();
        value = v.intern();
    }

    /**
     * Override the equals method. Two CoverageIntVdts are equal if
     * and only iff their respective attribute and value members are
     * equal.
     */
    public boolean equals(Object o) {
        if (o instanceof CoverageCharVdt) {
            CoverageCharVdt civ = (CoverageCharVdt) o;
            // we can use == rather than String.equals(String) since
            // attribute, and value are interned.
            return ((attribute == civ.attribute) && (value == civ.value));
        } else {
            return false;
        }
    }

    /**
     * Override hashcode. Compute a hashcode based on our member
     * values, rather than our (base class) object identity.
     */
    public int hashCode() {
        return (attribute.hashCode() ^ value.hashCode());
    }
}
