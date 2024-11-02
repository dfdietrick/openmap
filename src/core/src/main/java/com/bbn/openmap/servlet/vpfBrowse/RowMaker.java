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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/RowMaker.java,v $
// $Revision: 1.3 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.util.List;

import com.bbn.openmap.util.html.TableRowElement;

/**
 * An interface used to generate rows of an (html) table from a (VPF)
 * table.
 */
public interface RowMaker {
    /**
     * Generate an HTML table row from a vpf table row
     * 
     * @param row the VPF table row
     * @return the HTML representation
     */
    public TableRowElement generateRow(List row);

    /**
     * Used to indicate that no more calls to addList will be made.
     * (implementation may want to reclaim resources without waiting
     * for the finalizer to run.)
     */
    public void close();
}
