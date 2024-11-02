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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/ComplexJoinRowMaker.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.layer.vpf.FeatureClassInfo;
import com.bbn.openmap.util.html.TableDataElement;
import com.bbn.openmap.util.html.TableRowElement;

/**
 * A RowMaker subclass that handles joins between tables, where the
 * column guiding the join of the second table is not "id", and thus
 * not simply the row identifier.
 */
public class ComplexJoinRowMaker extends PlainRowMaker {
    /** the column that contains the foreign key to the primitive table */
    final int theColumn;
    /** the column that contains the tile identifier */
    final int tileColumn;
    /** a list reused to load join rows */
    final List jtrow = new ArrayList();

    /** a map from the table key to row id */
    final Map keyMap;
    /** the table we're joining with */
    final DcwRecordFile joinTable;

    public ComplexJoinRowMaker(DcwRecordFile table, String joinColumnName,
            String tableName, String tableKeyColumn, boolean isTiled)
            throws FormatException {
        theColumn = table.whatColumn(joinColumnName);
        tileColumn = table.whatColumn(FeatureClassInfo.TILE_ID_COLUMN_NAME);
        if (isTiled) {
            throw new FormatException("can't complex join with tiling (yet)");
        }
        joinTable = new DcwRecordFile(new File(table.getTableFile()).getParentFile()
                + File.separator + tableName);
        keyMap = getKeyMap(tableKeyColumn);
    }

    HashMap getKeyMap(String keyColumn) throws FormatException {
        int jcol = joinTable.whatColumn(keyColumn);
        HashMap retmap = new HashMap();
        while (joinTable.parseRow(jtrow)) {
            retmap.put(jtrow.get(jcol), jtrow.get(0));
        }
        return retmap;
    }

    public void addToRow(TableRowElement row, List l) {
        int i = 0;
        for (Iterator li = l.iterator(); li.hasNext();) {
            Object elt = li.next();
            if (i == theColumn) {
                Number wrow = (Number) keyMap.get(elt);
//                int tileId = (tileColumn == -1) ? -1
//                        : VPFUtil.objectToInt(l.get(tileColumn));

                try {
                    if (wrow == null) {
                        row.addElement("[" + elt + "]");
                    } else if (joinTable.getRow(jtrow, wrow.intValue())) {
                        for (Iterator it = jtrow.iterator(); it.hasNext();) {
                            row.addElement(new TableDataElement("CLASS=JoinColumn", it.next()
                                    .toString()));
                        }
                    } else {
                        row.addElement("Join failed!");
                    }
                } catch (FormatException fe) {
                    row.addElement(fe.toString());
                }
            } else {
                row.addElement(elt.toString());
            }
            i++;
        }
    }

    public void close() {
        joinTable.close();
    }
}
