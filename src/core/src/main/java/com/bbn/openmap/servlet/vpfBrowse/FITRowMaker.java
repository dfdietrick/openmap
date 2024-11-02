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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/FITRowMaker.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.layer.vpf.VPFUtil;
import com.bbn.openmap.util.html.TableDataElement;
import com.bbn.openmap.util.html.TableRowElement;

/**
 * A RowMaker that performs the join in a feature index table.
 */
public class FITRowMaker extends PlainRowMaker {
    /** a list reused to load primitive rows */
    final List primRow = new ArrayList();
    /** a list reused to load feature rows */
    final List featureRow = new ArrayList();

    /** the array of feature tables, each index is lazy-initialized */
    final DcwRecordFile[] featureTables;
    /** the array of feature names for the coverage */
    final String[] featureNames;

    /** the utility class that understands tiled and untiled data */
    final TileHolder tiler;

    /** the column in the FIT that specifies the primitive id */
    final int primIdColumn;
    /** the column in the FIT that specifies the tile id (may be -1) */
    final int tileIdColumn;
    /** the column in the FIT that specifies the feature class id */
    final int fcIdColumn;
    /** the column in the FIT that specifies the feature id */
    final int featureIdColumn;
    /** the path for the coverage */
    final File dirPath;
    /** the extension (e.g. .lft) used for feature table names */
    final String featureTableExt;

    /**
     * Construct a rowmaker for a feature index table.
     * 
     * @param drf the feature index table
     * @throws FormatException some error was encountered
     */
    public FITRowMaker(DcwRecordFile drf) throws FormatException {
        String tableName = drf.getTableName().substring(0, 3);
        dirPath = new File(drf.getTableFile()).getParentFile();

        featureTableExt = getExtensionForTable(tableName);

        primIdColumn = drf.whatColumn("prim_id");
        tileIdColumn = drf.whatColumn("tile_id");
        fcIdColumn = drf.whatColumn("fc_id");
        featureIdColumn = drf.whatColumn("feature_id");

        tiler = new TileHolder(dirPath, tableName, (tileIdColumn != -1));
        featureNames = getFeatureNames(dirPath);
        featureTables = new DcwRecordFile[featureNames.length];
    }

    /**
     * Returns the feature table that corresponds to the feature class
     * ID.
     * 
     * @param fcId the feature class ID
     * @return the feature table
     * @throws FormatException the feature table couldn't be created
     */
    public DcwRecordFile getFeatureTable(int fcId) throws FormatException {
        fcId -= 1; // array is 0-based, table ids are 1-based
        DcwRecordFile retval = featureTables[fcId];
        if (retval == null) {
            retval = new DcwRecordFile(dirPath + File.separator
                    + featureNames[fcId].toLowerCase() + featureTableExt);
            featureTables[fcId] = retval;
        }
        return retval;
    }

    public void addToRow(TableRowElement row, List l) {
        int primId = VPFUtil.objectToInt(l.get(primIdColumn));
        int tileId = (tileIdColumn == -1) ? -1
                : VPFUtil.objectToInt(l.get(tileIdColumn));
        int fcId = VPFUtil.objectToInt(l.get(fcIdColumn));
        int featureId = VPFUtil.objectToInt(l.get(featureIdColumn));
        int id = VPFUtil.objectToInt(l.get(0));
        row.addElement("" + id + " (" + tileId + "," + primId + ") (" + fcId
                + ", " + featureId + ")");
        try {
            tiler.getRow(tileId, primId, primRow);
            DcwRecordFile featureTable = getFeatureTable(fcId);
            featureTable.getRow(featureRow, featureId);
            for (Iterator i = primRow.iterator(); i.hasNext();) {
                row.addElement(new TableDataElement("CLASS=JoinColumn", i.next()
                        .toString()));
            }
            for (Iterator i = featureRow.iterator(); i.hasNext();) {
                row.addElement(new TableDataElement("CLASS=Join2Column", i.next()
                        .toString()));
            }
        } catch (FormatException fe) {
            row.addElement(fe.toString());
        }
    }

    public String[] getFeatureNames(File dirPath) throws FormatException {
        File fcafile = new File(dirPath, "fca");
        if (!fcafile.canRead()) {
            fcafile = new File(dirPath, "fca.");
        }
        DcwRecordFile fca = new DcwRecordFile(fcafile.toString());
        List l = new ArrayList(fca.getColumnCount());
        int fclassColumn = fca.whatColumn("fclass");
        List fclassnames = new ArrayList();
        while (fca.parseRow(l)) {
            fclassnames.add(l.get(fclassColumn));
        }
        fca.close();
        String retval[] = new String[fclassnames.size()];
        fclassnames.toArray(retval);
        return retval;
    }

    public static String getExtensionForTable(String tablename) {

        if (tablename.equals("fac")) {
            return ".aft";
        } else if (tablename.equals("cnd")) {
            return ".pft";
        } else if (tablename.equals("end")) {
            return ".pft";
        } else if (tablename.equals("txt")) {
            return ".tft";
        } else if (tablename.equals("edg")) {
            return ".lft";
        }
        return null;
    }

    public void close() {
        tiler.close();
        for (int i = 0; i < featureTables.length; i++) {
            DcwRecordFile drf = featureTables[i];
            if (drf != null) {
                drf.close();
            }
        }
    }
}
