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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/SpatialIndexServlet.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.DcwSpatialIndex;
import com.bbn.openmap.util.html.HtmlListElement;
import com.bbn.openmap.util.html.ListElement;
import com.bbn.openmap.util.html.TableHeaderElement;
import com.bbn.openmap.util.html.TableRowElement;
import com.bbn.openmap.util.html.WrapElement;

/**
 * This servlet generates HTML for VPF files in spatial index format.
 */
public class SpatialIndexServlet extends VPFHttpServlet {

    /**
     * A do-nothing constructor - init does all the work.
     */
    public SpatialIndexServlet() {
        super();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String filePath = (String) request.getAttribute(DispatchServlet.ROOTPATH_FILENAME);
        if (filePath == null) {
            String pathInfo = setPathInfo(request);
            filePath = contextInfo.resolvePath(pathInfo);
            if (!pathOkay(filePath, pathInfo, response)) {
                return;
            }
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        File fp = new File(filePath);
        String filename = fp.getName();
        String tableMatch = getIndexedTable(filename);
        if (tableMatch == null) {
            tableMatch = "non-standard spatial index";
        } else {
            tableMatch = fileURL(request,
                    response,
                    getRootDir(request),
                    tableMatch);
        }

        out.println(HTML_DOCTYPE + "<HTML><HEAD><TITLE>VPF Spatial Index "
                + filename + "</TITLE></HEAD>\r\n<BODY>\r\n<H1>Spatial Index "
                + filename + " for Table " + tableMatch + "</H1>\r\n");

        out.println(getStylesheetHTML(request));

        try {
            DcwSpatialIndex ff = new DcwSpatialIndex(filePath, false);
            printSpatial(request, response, ff);
            ff.close();
        } catch (FormatException fe) {
            out.println("FormatException while reading spatial index: "
                    + fe.getMessage());
        }
        out.println("</BODY></HTML>\r\n");
    }

    public void printSpatial(HttpServletRequest request,
                             HttpServletResponse response, DcwSpatialIndex si)
            throws FormatException, IOException {
        PrintWriter out = response.getWriter();
        out.println("<H3>General Spatial Index Information</H3>");
        HtmlListElement list = new HtmlListElement();
        list.addElement("Number Of Primitives: " + si.getNumberOfPrimitives());
        int nodesInTree = si.getNodesInTree();
        list.addElement("Nodes in Tree: " + nodesInTree);
        list.addElement("Bounding Rectangle: (" + si.getBoundingX1() + ", "
                + si.getBoundingY1() + ") - (" + si.getBoundingX2() + ", "
                + si.getBoundingY2() + ")");
        list.generate(out);
        out.println("<H3>Spatial Index Data</H3>");
        TableRowElement columnNames = new TableRowElement();
        columnNames.addElement(new TableHeaderElement("Primitive ID"));
        columnNames.addElement(new TableHeaderElement("x1"));
        columnNames.addElement(new TableHeaderElement("x2"));
        columnNames.addElement(new TableHeaderElement("y1"));
        columnNames.addElement(new TableHeaderElement("y2"));
        for (int i = 0; i < nodesInTree; i++) {
            int count = si.getPrimitiveCount(i);
            int offset = si.getPrimitiveOffset(i);
            DcwSpatialIndex.PrimitiveRecord pr[] = si.getPrimitiveRecords(i);

            out.println("<H4>Node " + i);
            if (count == 0) {
                out.println("(no primitives)</H4>\r\n");
            } else {
                out.println("</H4>Primitive Count:" + count
                        + " Relative Offset:" + offset + "\n");

                ListElement rows = new ListElement();
                WrapElement table = new WrapElement("table", "BORDER=1", rows);
                rows.addElement(columnNames);
                for (int j = 0; j < pr.length; j++) {
                    DcwSpatialIndex.PrimitiveRecord pr1 = pr[j];
                    TableRowElement datarow = new TableRowElement();
                    rows.addElement(datarow);
                    datarow.addElement(Integer.toString(pr1.primId));
                    datarow.addElement(Short.toString(pr1.x1));
                    datarow.addElement(Short.toString(pr1.x2));
                    datarow.addElement(Short.toString(pr1.y1));
                    datarow.addElement(Short.toString(pr1.y2));
                }
                table.generate(out);
            }
        }
    }

    /** a map from spatial index name to primitive file indexed */
    private HashMap indexTableMap;

    /**
     * Returns the name of the primitive file that the spatial index
     * is for.
     * 
     * @param indexName the name of the index
     * @return the name of the primitive file
     */
    public String getIndexedTable(String indexName) {
        if (indexTableMap == null) {
            HashMap newMap = new HashMap();
            newMap.put("esi", "edg");
            newMap.put("esi.", "edg.");
            newMap.put("fsi", "fac");
            newMap.put("fsi.", "fac.");
            newMap.put("csi", "cnd");
            newMap.put("csi.", "cnd.");
            newMap.put("nsi", "end");
            newMap.put("nsi.", "end.");
            newMap.put("tsi", "txt");
            newMap.put("tsi.", "txt.");
            indexTableMap = newMap;
        }
        return (String) indexTableMap.get(indexName);
    }
}
