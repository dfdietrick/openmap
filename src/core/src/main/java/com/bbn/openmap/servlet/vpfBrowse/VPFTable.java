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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/VPFTable.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.DcwRecordFile;

/**
 * Wrapper for DcwRecordFile objects, to give JSPs a way to interact
 * with them directly.
 */
public class VPFTable {

    private DcwRecordFile table;

    public VPFTable() {}

    public void setFile(String file) {
        try {
            if ((file == null) && (table != null)) {
                table.close();
                table = null;
            }
            if (file != null) {
                table = new DcwRecordFile(file);
            }
        } catch (FormatException fe) {
        }
    }

    public String getTablename() {
        return (table == null) ? "default" : table.getTableName();
    }

    public String getDescription() {
        return (table == null) ? "default" : table.getDescription();
    }
}
