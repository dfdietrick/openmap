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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/Schema.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.layer.vpf.DcwColumnInfo;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.util.html.HtmlListElement;
import com.bbn.openmap.util.html.ListElement;
import com.bbn.openmap.util.html.StringElement;
import com.bbn.openmap.util.html.TableRowElement;
import com.bbn.openmap.util.html.WrapElement;

/**
 * A servlet class that will print the schema for a VPF table.
 */
public class Schema extends VPFHttpServlet {

    /**
     * A do-nothing constructor - init does all the work.
     */
    public Schema() {
        super();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        DcwRecordFile foo = (DcwRecordFile) request.getAttribute(DispatchServlet.RECORD_FILE_OBJ);
        if (foo == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String basepath = getRootDir(request);
        String url = response.encodeURL(request.getContextPath()
                + "/VPFHelp.jsp?topic=table_schema");
        PrintWriter out = response.getWriter();
        out.println("<H2>Table Schema</H2>");
        out.println("<H3>General Table Information</H3>");
        HtmlListElement list = new HtmlListElement();
        list.addElement("Table Name: " + foo.getTableName());
        list.addElement("Table Description: " + foo.getDescription());
        list.addElement("DocFile Name: "
                + fileURL(request,
                        response,
                        basepath,
                        foo.getDocumentationFilename()));
        int reclen = foo.getRecordLength();
        String reclenstr = (reclen == -1) ? ("variable")
                : (Integer.toString(reclen) + " bytes");
        list.addElement("Record Length: " + reclenstr);
        try {
            list.addElement("Record Count: " + foo.getRecordCount());
        } catch (com.bbn.openmap.io.FormatException fe) {
            list.addElement("Record Count Error: " + fe.toString());
        }
        list.generate(out);

        // out.println("<br><H3>Column Schema</H3>");
        ListElement rows = new ListElement();
        WrapElement table = new WrapElement("table", "BORDER=1", rows);
        TableRowElement thr = new TableRowElement();
        rows.addElement(new WrapElement("CAPTION", new StringElement("Column Schema")));
        rows.addElement(thr);
        thr.addElement(THE("#", url));
        thr.addElement(THE("Name", url));
        thr.addElement(THE("Type", url));
        thr.addElement(THE("Count", url));
        thr.addElement(THE("Key Type", url));
        thr.addElement(THE("Description", url));
        thr.addElement(THE("VDT", url));
        thr.addElement(THE("Thematic Index", url));
        thr.addElement(THE("DocFile", url));
        DcwColumnInfo dci[] = foo.getColumnInfo();
        for (int i = 0; i < dci.length; i++) {
            TableRowElement tr = new TableRowElement();
            rows.addElement(tr);
            tr.addElement(Integer.toString(i));
            tr.addElement(dci[i].getColumnName());
            tr.addElement(String.valueOf(dci[i].getFieldType()));
            int elts = dci[i].getNumberOfElements();
            tr.addElement(elts == -1 ? "*" : Integer.toString(elts));
            tr.addElement(String.valueOf(dci[i].getKeyType()));
            tr.addElement(dci[i].getColumnDescription());
            tr.addElement(fileURL(request, response, basepath, dci[i].getVDT()));
            tr.addElement(thematicURL(request,
                    response,
                    basepath,
                    dci[i].getThematicIndexName()));
            tr.addElement(docURL(request,
                    response,
                    basepath,
                    dci[i].getNarrativeTable()));
        }
        table.generate(response.getWriter());
    }

    public static String thematicURL(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String pathname, String filename) {
        return toURL(request, response, "/Thematic", pathname, filename);
    }

    public static String docURL(HttpServletRequest request,
                                HttpServletResponse response, String pathname,
                                String filename) {
        return toURL(request, response, "/DocFile", pathname, filename);
    }
}
