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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/SpatialGraphicServlet.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
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

/**
 * This servlet generates HTML for VPF files in spatial index format.
 */
public class SpatialGraphicServlet extends VPFHttpServlet {

    /**
     * A do-nothing constructor - init does all the work.
     */
    public SpatialGraphicServlet() {
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

        response.setContentType("image/gif");

        int width = 200;
        int height = 200;
        int imageType = BufferedImage.TYPE_INT_ARGB;
        BufferedImage bufferedImage = new BufferedImage(width, height, imageType);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setClip(0, 0, width, height);
        g2d.setColor(Color.red);
        g2d.drawLine(10, 10, 95, 95);
        g2d.drawLine(105, 105, 190, 190);
        g2d.drawRect(1, 1, 198, 198);
        g2d.dispose();
        // byte [] imageData = AcmeGifHelper.encodeGif(bufferedImage);

        // ServletOutputStream sos = response.getOutputStream();
        // sos.write(imageData);

        String filename = filePath;
        String tableMatch = getIndexedTable(filename);
        if (tableMatch == null) {
            tableMatch = "non-standard spatial index";
        } else {
            tableMatch = fileURL(request,
                    response,
                    getRootDir(request),
                    tableMatch);
        }

        try {
            DcwSpatialIndex ff = new DcwSpatialIndex(filePath.toString(), false);
            printSpatial(request, response, ff);
            ff.close();
        } catch (FormatException fe) {
        }
    }

    public void printSpatial(HttpServletRequest request,
                             HttpServletResponse response, DcwSpatialIndex si)
            throws com.bbn.openmap.io.FormatException, IOException {
        int width = 200;
        int height = 200;
        int imageType = BufferedImage.TYPE_INT_ARGB;
        BufferedImage bufferedImage = new BufferedImage(width, height, imageType);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setClip(0, 0, width, height);
        g2d.setColor(Color.red);
        g2d.drawLine(10, 10, 95, 95);
        g2d.drawLine(105, 105, 190, 190);
        g2d.drawRect(1, 1, 198, 198);
        g2d.dispose();
        // byte [] imageData = AcmeGifHelper.encodeGif(bufferedImage);

        // ServletOutputStream sos = response.getOutputStream();
        // sos.write(imageData);

        HtmlListElement list = new HtmlListElement();
        list.addElement("Number Of Primitives: " + si.getNumberOfPrimitives());
        int nodesInTree = si.getNodesInTree();
        list.addElement("Nodes in Tree: " + nodesInTree);
        list.addElement("Bounding Rectangle: (" + si.getBoundingX1() + ", "
                + si.getBoundingY1() + ") - (" + si.getBoundingX2() + ", "
                + si.getBoundingY2() + ")");
        TableRowElement columnNames = new TableRowElement();
        columnNames.addElement(new TableHeaderElement("Primitive ID"));
        columnNames.addElement(new TableHeaderElement("x1"));
        columnNames.addElement(new TableHeaderElement("x2"));
        columnNames.addElement(new TableHeaderElement("y1"));
        columnNames.addElement(new TableHeaderElement("y2"));
        for (int i = 0; i < nodesInTree; i++) {
            int count = si.getPrimitiveCount(i);
            //int offset = si.getPrimitiveOffset(i);
            DcwSpatialIndex.PrimitiveRecord pr[] = si.getPrimitiveRecords(i);

            if (count == 0) {
            } else {
                ListElement rows = new ListElement();
                //WrapElement table = new WrapElement("table", "BORDER=1", rows);
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
