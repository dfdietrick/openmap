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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/DispatchServlet.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.DcwRecordFile;

/**
 * This class infers the format of a VPF file from the name of the file, and
 * dispatches to the appropriate servlet for that type.
 * 
 * This could probably also be handled by a long set of servlet-mapping tags in
 * the deployment descriptor. (web.xml)
 */
public class DispatchServlet
        extends VPFHttpServlet {
    public static final String RECORD_FILE_OBJ = "com.bbn.openmap.vpf_tools.table_obj";
    public static final String ROOTPATH_FILENAME = "com.bbn.openmap.vpf_tools.url_path";

    /**
     * A do-nothing constructor - init does all the work.
     */
    public DispatchServlet() {
        super();
    }

    /**
     * Just a test main to parse vpf datafiles
     * 
     * param args files to parse, plus other command line flags
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = setPathInfo(request);
        String rootpath = contextInfo.resolvePath(pathInfo);
        if (!pathOkay(rootpath, pathInfo, response)) {
            return;
        }

        PrintWriter out = response.getWriter();
        File rp = new File(rootpath);
        String filename = rp.getName().toLowerCase();

        String end = "</BODY></HTML>\r\n";

        // ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // PrintStream s = new PrintStream(bos);
        // System.setOut(s);

        request.setAttribute(ROOTPATH_FILENAME, rootpath);

        try {
            if (rp.isDirectory()) {
                RequestDispatcher rd = request.getRequestDispatcher("/DirectoryList");
                rd.forward(request, response);
                return;
            } else if (filename.endsWith("x") || filename.endsWith("x.")) {
                response.setContentType("text/html");
                out.println(HTML_DOCTYPE + "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD>\r\n<BODY>\r\n<H1>Table " + filename
                        + "</H1>\r\n");
                out.println(getStylesheetHTML(request));
                out.println("Skipping VLI format - this format is simply an index to find rows in a corresponding table file, it isn't very interesting to look at so its getting skipped.");
            } else if (filename.endsWith("ti")) {
                RequestDispatcher rd = request.getRequestDispatcher("/Thematic");
                rd.forward(request, response);
            } else if (filename.endsWith("si") || filename.endsWith("si.")) {
                RequestDispatcher rd = request.getRequestDispatcher("/SpatialIndex");
                rd.forward(request, response);
            } else if (filename.endsWith(".doc")) {
                RequestDispatcher rd = request.getRequestDispatcher("/DocFile");
                rd.forward(request, response);
            } else {
                response.setContentType("text/html");
                out.println(HTML_DOCTYPE + "<HTML>\n<HEAD><TITLE>" + filename + "</TITLE></HEAD>\r\n<BODY>\r\n<H1>Table "
                        + filename + "</H1>\r\n");
                out.println(getStylesheetHTML(request));
                DcwRecordFile foo = new DcwRecordFile(rootpath);
                request.setAttribute(RECORD_FILE_OBJ, foo);
                RequestDispatcher rd = request.getRequestDispatcher("/Schema");
                rd.include(request, response);
                RequestDispatcher rd2 = request.getRequestDispatcher("/Data");
                rd2.include(request, response);

                foo.close();
            }
        } catch (FormatException f) {
            throw new ServletException("Format Error: ", f);
        }
        // s.close();
        out.println("<pre>");
        out.println("Context Path: " + request.getContextPath());
        out.println("PathInfo: " + request.getPathInfo());
        out.println("ServletPath: " + request.getServletPath());
        out.println("Query String: " + request.getQueryString());
        // out.print(bos.toString());
        out.println("</pre>" + end);
    }

}
