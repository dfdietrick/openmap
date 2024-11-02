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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/TableSubsetRecordIterator.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.DcwRecordFile;

/**
 * An iterator that will return a subset of the rows in a table.
 */
public class TableSubsetRecordIterator implements Iterator {
    final private int vals[];
    final DcwRecordFile drf;
    final List l;
    private int current = 0;

    public TableSubsetRecordIterator(int vals[], DcwRecordFile drf, List l) {
        this.vals = vals;
        this.drf = drf;
        this.l = l;
    }

    /**
     * Constructor
     * 
     * @param drf the table to parse
     * @param vals the row numbers to be returned
     */
    public TableSubsetRecordIterator(int vals[], DcwRecordFile drf) {
        this(vals, drf, new ArrayList(drf.getColumnCount()));
    }

    public boolean hasNext() {
        return (current < vals.length);
    }

    public Object next() {
        boolean gotit;
        try {
            gotit = drf.getRow(l, vals[current++]);
        } catch (FormatException fe) {
            System.out.println("fe: " + fe);
            gotit = false;
        }
        return gotit ? l : null;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
