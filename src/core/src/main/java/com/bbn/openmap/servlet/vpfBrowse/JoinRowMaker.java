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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/JoinRowMaker.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.layer.vpf.FeatureClassInfo;
import com.bbn.openmap.layer.vpf.TilingAdapter;
import com.bbn.openmap.util.html.TableDataElement;
import com.bbn.openmap.util.html.TableRowElement;

/**
 * A RowMaker subclass that handles simple joins between tables.
 */
public class JoinRowMaker extends PlainRowMaker {
    /** the column that contains the foreign key to the primitive table */
    final int theColumn;
    /** the column that contains the tile identifier */
    final int tileColumn;
    /** a list reused to load primitive rows */
    final List jtrow = new ArrayList();

    /** the utility class that understands tiled and untiled data */
    final TileHolder tiler;
    final TilingAdapter ta;

    public JoinRowMaker(DcwRecordFile table, String joinColumnName,
            String tableName, boolean isTiled) throws FormatException {
        theColumn = table.whatColumn(joinColumnName);
        tileColumn = isTiled ? table.whatColumn(FeatureClassInfo.TILE_ID_COLUMN_NAME)
                : -1;
        ta = table.getTilingAdapter(tileColumn, theColumn);
        tiler = new TileHolder(new File(table.getTableFile()).getParentFile(), tableName, isTiled);
    }

    public void addToRow(TableRowElement row, List l) {
        int i = 0;
        for (Iterator li = l.iterator(); li.hasNext();) {
            Object elt = li.next();
            if (i == theColumn) {
                int whatrow = ta.getTilePrimId(l);
                int tileId = ta.getTileId(l);
                try {
                    if (tiler.getRow(ta, l, jtrow)) {
                        for (Iterator it = jtrow.iterator(); it.hasNext();) {
                            row.addElement(new TableDataElement("CLASS=JoinColumn", it.next()
                                    .toString()));
                        }
                    } else {
                        row.addElement("Join failed! [" + elt + "]" + "("
                                + tileId + "," + whatrow + ")");
                    }
                } catch (FormatException fe) {
                    row.addElement(fe.toString() + "(" + tileId + "," + whatrow
                            + ")");
                }
            } else {
                row.addElement(elt.toString());
            }
            i++;
        }
    }

    public void close() {
        tiler.close();
    }
}
