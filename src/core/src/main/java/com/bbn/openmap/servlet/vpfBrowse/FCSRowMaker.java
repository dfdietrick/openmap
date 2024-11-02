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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/FCSRowMaker.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.layer.vpf.Constants;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.util.html.TableRowElement;

/**
 * A RowMaker class specifically for the markup of VPF feature class schema
 * (fcs) files.
 */
public class FCSRowMaker extends ReferenceRowMaker {
    /** the length of a row (number of columns) */
    final int rowLen;
    /** the directory of the file being read */
    final File drfp;
    final String basepath;
    final int idCol;
    final int featureClassCol;
    final int table1Col;
    final int table1keyCol;
    final int table2Col;
    final int table2keyCol;

    public FCSRowMaker(HttpServletRequest request,
                       HttpServletResponse response,
                       String basepath, DcwRecordFile drf) {
        super(request, response);
        this.rowLen = drf.getColumnCount();
        this.basepath = basepath;
        this.drfp = new File(drf.getTableFile()).getParentFile();
        idCol = drf.whatColumn(Constants.ID);
        featureClassCol = drf.whatColumn(Constants.FCS_FEATURECLASS);
        table1Col = drf.whatColumn(Constants.FCS_TABLE1);
        table1keyCol = drf.whatColumn(Constants.FCS_TABLE1KEY);
        table2Col = drf.whatColumn(Constants.FCS_TABLE2);
        table2keyCol = drf.whatColumn(Constants.FCS_TABLE2KEY);
    }

    public void addToRow(TableRowElement row, List l) {
        int rv = ((Number)l.get(idCol)).intValue();
        String table1 = ((String)l.get(table1Col)).toLowerCase();
        String table1key = ((String)l.get(table1keyCol)).toLowerCase();
        String table2 = ((String)l.get(table2Col)).toLowerCase();
        String table2key = ((String)l.get(table2keyCol)).toLowerCase();
        File fn = new File(drfp, table1);
        File otf = new File(drfp, table2);
        boolean tiled = !otf.exists();
        for (int i = 0; i < rowLen; i++) {
            if (i == idCol) {
                if (fn.exists()) {
                    row.addElement(Data.joinURL(request, response,
                                                rv, table1,
                                                table1key, table2,
                                                table2key,
                                                tiled));
                } else {
                    row.addElement(Integer.toString(rv));
                }
            } else if ((i==table1Col) || (i==table2Col)) {
                String tablename = ((String)l.get(i)).toLowerCase();
                if (new File(drfp, tablename).exists()) {
                    row.addElement(fileURL(basepath, tablename));
//              } else if (Constants.endTableName.equals(tablename) ||
//                         Constants.cndTableName.equals(tablename) ||
//                         Constants.faceTableName.equals(tablename) ||
//                         "edg".equals(tablename) ||
//                         "txt".equals(tablename)) {
//                  TileHolder ta = new TileHolder(new File(basepath), tablename, true);
//                  row.addElement(tablename);
                } else {
                    row.addElement(tablename);
                }
            } else {
                row.addElement(l.get(i).toString().toLowerCase());
            }
        }
    }
}
