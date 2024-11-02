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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/LibraryBean.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.LibrarySelectionTable;

/**
 * This class prints out a description of a VPF database, listing the
 * available libraries, coverage types and feature types.
 */
public class LibraryBean {
    private LibrarySelectionTable lst;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public void setContext(ServletContext c) {
        contextInfo = ContextInfo.getContextInfo(c);
    }

    public void setResponse(HttpServletResponse r) {
        response = r;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public LibrarySelectionTable getLst() {
        return lst;
    }

    public String getLibName() {
        return (lst == null) ? "unknown" : lst.getDatabaseName();
    }

    public void setPath(String pathInfo) {
        if (pathInfo == null) {
            return;
        }
        int findex = pathInfo.indexOf('/', 1);
        if (findex < 0) {
            findex = pathInfo.length();
        }
        String libname = pathInfo.substring(0, findex);

        try {
            lst = getLST(libname);
            if (lst == null) {
                return;
            }
            // String dbname = lst.getDatabaseName();
            // out.println("<HTML>\n<HEAD><TITLE>Describe VPF Database
            // " +
            // dbname + "</TITLE></HEAD>\n<BODY>\n<H1>VPF Database " +
            // dbname + "</H1>\n");
            // ListElement dble = new ListBodyElement();
            // WrapElement dblist = new WrapElement("ul", dble);
            // dble.addElement("Database Description: " +
            // lst.getDatabaseDescription());
            // dble.addElement("Database Description Table: " +
            // buildURL(request, response, libname, "dht"));
            // String[] libraries = lst.getLibraryNames();
            // StringBuffer libnames = new StringBuffer("Database
            // Libraries: ");
            // for (int i = 0; i < libraries.length; i++) {
            // libnames.append("<A HREF=\"#").append(libraries[i]);
            // libnames.append("\">").append(libraries[i]);
            // libnames.append("</A>").append(" ");
            // }
            // libnames.append("(from ");
            // libnames.append(buildURL(request, response, libname,
            // "lat"));
            // libnames.append(")");

            // dble.addElement(libnames.toString());
            // dblist.generate(out);
            // for (int i = 0; i < libraries.length; i++) {
            // String prefix = libraries[i] + ":";
            // printLibrary(request, response, libname,
            // lst.getCAT(libraries[i]));
            // }
            // out.println("</body></html>");
        } catch (FormatException fe) {
            // throw new ServletException("FormatException: " , fe);
        }
    }

    /** the context object used for config info */
    protected ContextInfo contextInfo;

    public LibrarySelectionTable getLST(String libname) throws FormatException {
        LibrarySelectionTable lst = contextInfo.getLST(libname);
        if (lst == null) {
            String lib_home = contextInfo.getPath(libname);
            if (lib_home == null) {
                return null;
            }

            lst = new LibrarySelectionTable(lib_home);
            contextInfo.putLST(libname, lst);
        }
        return lst;
    }
}
