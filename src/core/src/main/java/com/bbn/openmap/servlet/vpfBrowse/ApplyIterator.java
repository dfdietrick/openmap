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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/ApplyIterator.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.util.Iterator;

/**
 * An Iterator subclass that wraps another iterator. Each element
 * returned by the iterator is the result of calling Applyable.apply()
 * on the current element of the underlying iterator.
 */
public class ApplyIterator implements Iterator {
    /** the iterator to be wrapped */
    final private Iterator wrapped;
    /** the Applyable object to use before returning each element */
    final private Applyable applier;

    /**
     * Constructor
     * 
     * @param iter the iterator to wrap, may not be null
     * @param apply the Applyable object to use in next(), may not be
     *        null
     */
    public ApplyIterator(Iterator iter, Applyable apply) {
        wrapped = iter;
        applier = apply;
    }

    public boolean hasNext() {
        return wrapped.hasNext();
    }

    public Object next() {
        return applier.apply(wrapped.next());
    }

    public void remove() {
        wrapped.remove();
    }
}
