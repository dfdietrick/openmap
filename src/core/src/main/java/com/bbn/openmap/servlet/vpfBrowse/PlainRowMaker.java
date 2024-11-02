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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/PlainRowMaker.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.util.Iterator;
import java.util.List;

import com.bbn.openmap.util.html.TableRowElement;

/**
 * A basic RowMaker that makes rows.
 */
public class PlainRowMaker implements RowMaker {

    /**
     * A basic constructor that doesn't do anything special.
     */
    public PlainRowMaker() {}

    /**
     * Generates a TableRowElement from a table by creating a new
     * TableRow and passing it and the list to addToRow, then
     * returning the new row.
     * 
     * @param l the VPF table row
     * @return a HTML representation of the VPF row
     */
    public TableRowElement generateRow(List l) {
        TableRowElement tr = new TableRowElement();
        addToRow(tr, l);
        return tr;
    }

    /**
     * Adds the elements of the list to the table row
     * 
     * @param row the HTML row
     * @param l the VPF row
     */
    public void addToRow(TableRowElement row, List l) {
        for (Iterator li = l.iterator(); li.hasNext();) {
            row.addElement(li.next().toString());
        }
    }

    /**
     * An empty implementation, since this class doesn't hold any
     * resources.
     */
    public void close() {}
}
