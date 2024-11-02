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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/VDTRowMaker.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.util.html.TableRowElement;

/**
 * A RowMaker class for int.vdt and char.vdt tables. It generates a
 * URL for columns that reference another table.
 */
public class VDTRowMaker extends ReferenceRowMaker {

    /** the path to the coverage */
    final String basepath;
    /** the column with the table name */
    final int tableCol;

    public VDTRowMaker(HttpServletRequest request,
            HttpServletResponse response, String basepath, DcwRecordFile drf) {
        super(request, response);
        this.basepath = basepath;
        tableCol = drf.whatColumn("table");
    }

    public void addToRow(TableRowElement row, List l) {
        int i = 0;
        for (Iterator li = l.iterator(); li.hasNext();) {
            Object elt = li.next();
            if (i == tableCol) {
                row.addElement(fileURL(basepath, ((String) elt).toLowerCase()));
            } else {
                row.addElement(elt.toString());
            }
            i++;
        }
    }
}
