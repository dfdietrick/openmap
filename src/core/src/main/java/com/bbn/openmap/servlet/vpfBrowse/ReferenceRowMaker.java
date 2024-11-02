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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/ReferenceRowMaker.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A RowMaker class that retains references to the HttpServletRequest
 * and HttpServletResponse instances of the request.
 */
public abstract class ReferenceRowMaker extends PlainRowMaker {
    /** the servlet request object */
    final protected HttpServletRequest request;
    /** the servlet response object */
    final protected HttpServletResponse response;

    public ReferenceRowMaker(HttpServletRequest request,
            HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public String toURL(String servletName, String pathname, String filename) {
        return VPFHttpServlet.toURL(request,
                response,
                servletName,
                pathname,
                filename);
    }

    public String fileURL(String pathname, String filename) {
        return VPFHttpServlet.fileURL(request, response, pathname, filename);
    }
}
