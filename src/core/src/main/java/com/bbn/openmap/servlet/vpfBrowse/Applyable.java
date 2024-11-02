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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/Applyable.java,v $
// $Revision: 1.2 $ $Date: 2004/10/14 18:06:32 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

/**
 * Applyable interface for use with ApplyIterator.
 */
public interface Applyable {
    /**
     * The apply method
     */
    Object apply(Object obj);
}
