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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/DescribeDBServlet.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.CoverageAttributeTable;
import com.bbn.openmap.layer.vpf.CoverageTable;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.layer.vpf.FeatureClassInfo;
import com.bbn.openmap.layer.vpf.LibrarySelectionTable;
import com.bbn.openmap.util.html.HtmlListElement;
import com.bbn.openmap.util.html.ListBodyElement;
import com.bbn.openmap.util.html.ListElement;
import com.bbn.openmap.util.html.WrapElement;

/**
 * This class prints out a description of a VPF database, listing the available
 * libraries, coverage types and feature types.
 */

public class DescribeDBServlet
        extends VPFHttpServlet {
    /**
     * Takes path arguments, and prints the DB it finds
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println(HTML_DOCTYPE);
        out.println(getStylesheetHTML(request));

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            selectDB(request, response);
            return;
        }
        int findex = pathInfo.indexOf('/', 1);
        if (findex < 0) {
            findex = pathInfo.length();
        }
        String libname = pathInfo.substring(1, findex);

        try {
            LibrarySelectionTable lst = getLST(libname);
            if (lst == null) {
                out.println("VPF Database not configured: " + libname);
                return;
            }
            String dbname = lst.getDatabaseName();
            out.println("<HTML>\n<HEAD><TITLE>Describe VPF Database " + dbname + "</TITLE></HEAD>\n<BODY>\n<H1>VPF Database "
                    + dbname + "</H1>\n");
            ListElement dble = new ListBodyElement();
            WrapElement dblist = new WrapElement("ul", dble);
            dble.addElement("Database Description: " + lst.getDatabaseDescription());
            dble.addElement("Database Description Table: " + buildURL(request, response, libname, "dht"));
            List<String> libraries = lst.getLibraryNames();
            StringBuffer libnames = new StringBuffer("Database Libraries: ");
            for (String libName : libraries) {
                libnames.append("<A HREF=\"#").append(libName);
                libnames.append("\">").append(libName);
                libnames.append("</A>").append(" ");
            }
            libnames.append("(from ");
            libnames.append(buildURL(request, response, libname, "lat"));
            libnames.append(")");

            dble.addElement(libnames.toString());
            dblist.generate(out);
            for (String libName : libraries) {
                // String prefix = libraries[i] + ":";
                printLibrary(request, response, libname, lst.getCAT(libName));
            }
            out.println("</body></html>");
        } catch (FormatException fe) {
            throw new ServletException("FormatException: ", fe);
        }
    }

    public void selectDB(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        PrintWriter out = response.getWriter();

        Set databases = contextInfo.keySet();

        out.println("<HTML>\n<HEAD><TITLE>Select VPF Database" + "</TITLE></HEAD>\n<BODY>\n<H1>Available VPF Databases" + "</H1>\n");
        HtmlListElement dblist = new HtmlListElement();
        for (Iterator dbi = databases.iterator(); dbi.hasNext();) {
            String db = (String) dbi.next();
            String url = request.getContextPath() + "/DescribeVPF/" + db;
            dblist.addElement("<A HREF=\"" + response.encodeURL(url) + "\">" + db + "</A>\r\n");
        }
        dblist.generate(out);
        out.println("</BODY></HTML>\r\n");
    }

    public static String buildURL(HttpServletRequest request, HttpServletResponse response, String filepref, String filename,
                                  String tag) {
        String url = request.getContextPath() + "/UnknownType/" + filepref + "/" + filename;
        return "<A HREF=\"" + response.encodeURL(url) + "\">" + tag + "</A>";
    }

    public static String buildURL(HttpServletRequest request, HttpServletResponse response, String filepref, String filename) {
        return buildURL(request, response, filepref, filename, filename);
    }

    /**
     * Prints a VPF Library.
     * 
     * @param request the HttpServletRequest.
     * @param response the HTTPServletResponse.
     * @param pathPrefix lines get printed with this prefix
     * @param cat the CoverageAttributeTable (Library) to print
     */
    public void printLibrary(HttpServletRequest request, HttpServletResponse response, String pathPrefix, CoverageAttributeTable cat)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        if (cat == null) {
            out.println("<H2>Catalog doesn't exist</H2>");
            return;
        }
        String libName = cat.getLibraryName();
        String libpath = pathPrefix + "/" + libName;

        out.println("<H2>Library <A NAME=\"" + libName + "\"></A>" + buildURL(request, response, pathPrefix, libName, libName)
                + "</H2>");
        String[] coverages = cat.getCoverageNames();
        Arrays.sort(coverages);

        HtmlListElement list = new HtmlListElement();
        list.addElement("Library uses " + (cat.isTiledData() ? "tiled" : "untiled") + " data");
        HtmlListElement clist = new HtmlListElement("Coverage names (from " + buildURL(request, response, libpath, "cat") + ")");

        list.addElement(clist);
        for (int i = 0; i < coverages.length; i++) {
            clist.addElement("<A HREF=\"#" + libName + "_" + coverages[i] + "\">" + coverages[i] + "</A>");
        }
        list.addElement("Library Header Table: " + buildURL(request, response, libpath, "lht"));
        list.addElement("Geographic Reference Table: " + buildURL(request, response, libpath, "grt"));
        list.generate(out);
        for (int i = 0; i < coverages.length; i++) {
            printCoverage(request, response, libpath + "/" + coverages[i], libName, cat, coverages[i]);
        }
    }

    /**
     * Prints a VPF Coverage
     * 
     * @param pathPrefix lines get printed with this prefix
     * @param cat the CoverageAttributeTable to get the Coverage from
     * @param covname the name of the coverage to print
     */
    public void printCoverage(HttpServletRequest request, HttpServletResponse response, String pathPrefix, String libName,
                              CoverageAttributeTable cat, String covname)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        out.println("<H3><A NAME=\"" + libName + "_" + covname + "\">Coverage "
                + buildURL(request, response, pathPrefix, "", covname) + " for Library <A HREF=\"#" + libName + "\">" + libName
                + "</A></H3>");
        HtmlListElement list = new HtmlListElement();
        list.addElement("Description: " + cat.getCoverageDescription(covname));
        list.addElement("Topology Level: " + cat.getCoverageTopologyLevel(covname));
        String fcsURL = buildURL(request, response, pathPrefix, "fcs?" + Data.RowSelectParam + "=" + Data.RowSelectAll, "fcs");
        list.addElement("Feature Class Schema: " + fcsURL);
        CoverageTable ct = cat.getCoverageTable(covname);
        // CoverageTable opens alot of files, go through and close
        // them
        for (Iterator i = ct.getFeatureClasses().values().iterator(); i.hasNext();) {
            FeatureClassInfo fci = (FeatureClassInfo) i.next();
            fci.close();
        }

        Map ftypeinfo = new TreeMap(ct.getFeatureTypeInfo());
        if (ftypeinfo.size() == 0) {
            list.addElement("No Feature Types in FCA");
        } else {
            HtmlListElement flist =
                    new HtmlListElement("Feature Types (from " + buildURL(request, response, pathPrefix, "fca") + ")");
            list.addElement(flist);
            for (Iterator i = ftypeinfo.values().iterator(); i.hasNext();) {
                CoverageTable.FeatureClassRec fcr = (CoverageTable.FeatureClassRec) i.next();
                String name = fcr.feature_class.toLowerCase();
                // char t = fcr.type;
                String desc = fcr.description;
                String tstring = "[unknown] ";
                String suffix = "";
                switch (fcr.type) {
                    case CoverageTable.TEXT_FEATURETYPE:
                        tstring = "[text feature] ";
                        suffix = ".tft";
                        break;
                    case CoverageTable.EDGE_FEATURETYPE:
                        tstring = "[edge feature] ";
                        suffix = ".lft";
                        break;
                    case CoverageTable.AREA_FEATURETYPE:
                        tstring = "[area feature] ";
                        suffix = ".aft";
                        break;
                    case CoverageTable.UPOINT_FEATURETYPE:
                        FeatureClassInfo fci = ct.getFeatureClassInfo(name);
                        char type = (fci != null) ? fci.getFeatureType() : CoverageTable.SKIP_FEATURETYPE;
                        if (type == CoverageTable.EPOINT_FEATURETYPE) {
                            tstring = "[entity point feature] ";
                        } else if (type == CoverageTable.CPOINT_FEATURETYPE) {
                            tstring = "[connected point feature] ";
                        } else {
                            tstring = "[missing point feature] ";
                        }
                        suffix = ".pft";
                        break;
                    case CoverageTable.COMPLEX_FEATURETYPE:
                        tstring = "[complex feature] ";
                        suffix = ".cft";
                        break;
                    default:
                        tstring = "[unknown] ";
                        suffix = "";
                }
                String url = buildURL(request, response, pathPrefix, name + suffix, name);
                flist.addElement(url + ": " + tstring + desc);
            }
        }
        try {
            HtmlListElement flist = new HtmlListElement("Feature Types (from " + fcsURL + ")");
            boolean generateflist = false;
            DcwRecordFile fcs = new DcwRecordFile(ct.getDataPath() + File.separator + "fcs" + (ct.appendDot ? "." : ""));
            int featureClassColumn = fcs.whatColumn("feature_class");
            int table1Column = fcs.whatColumn("table1");
            // int table1_keyColumn = fcs.whatColumn("table1_key");
            // int table2Column = fcs.whatColumn("table2");
            // int table2_keyColumn = fcs.whatColumn("table2_key");

            List fcsl = new ArrayList(fcs.getColumnCount());
            while (fcs.parseRow(fcsl)) {
                String featureclass = ((String) fcsl.get(featureClassColumn)).toLowerCase();
                String table1 = ((String) fcsl.get(table1Column)).toLowerCase();
                if (!ftypeinfo.containsKey(featureclass)) {
                    ftypeinfo.put(featureclass, null);
                    String type = null;
                    if (table1.endsWith(".cft")) {
                        type = "complex feature";
                    } else if (table1.endsWith(".pft")) {
                        type = "point feature";
                    } else if (table1.endsWith(".lft")) {
                        type = "line feature";
                    } else if (table1.endsWith(".aft")) {
                        type = "area feature";
                    } else if (table1.endsWith(".tft")) {
                        type = "text feature";
                    }
                    if (type != null) {
                        generateflist = true;
                        flist.addElement(type + " " + buildURL(request, response, pathPrefix, table1, featureclass));
                    }
                }
            }
            if (generateflist) {
                list.addElement(flist);
            }
            fcs.close();
        } catch (FormatException fe) {
            list.addElement("no fcs");
        }
        list.generate(out);
    }

    public LibrarySelectionTable getLST(String libname)
            throws FormatException {
        LibrarySelectionTable lst = contextInfo.getLST(libname);
        if (lst == null) {
            String lib_home = contextInfo.getPath(libname);
            if (lib_home == null) {
                return null;
            }
            // File flib_home = new File(lib_home);

            lst = new LibrarySelectionTable(lib_home);
            contextInfo.putLST(libname, lst);
        }
        return lst;
    }
}
