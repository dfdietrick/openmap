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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/Data.java,v $
// $Revision: 1.6 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.io.FormatException;
import com.bbn.openmap.layer.vpf.Constants;
import com.bbn.openmap.layer.vpf.DcwColumnInfo;
import com.bbn.openmap.layer.vpf.DcwRecordFile;
import com.bbn.openmap.util.html.ListElement;
import com.bbn.openmap.util.html.StringElement;
import com.bbn.openmap.util.html.TableHeaderElement;
import com.bbn.openmap.util.html.TableRowElement;
import com.bbn.openmap.util.html.WrapElement;

/**
 * A servlet class that will output table data.
 */
public class Data extends VPFHttpServlet {
    /** the name of the http parameter with the table name */
    public static final String VDTParam = "vdt";
    /** the possible values of the rowselect parameter */
    public static final String RowSelectParam = "show";
    public static final String RowSelectAll = "all";
    public static final String RowSelectNone = "none";
    public static final String RowSelectTest = "test";
    /** other parameters that the servlet takes */
    public static final String JoinColumnParam = "colname";
    public static final String JoinOtherTableParam = "othertable";
    public static final String JoinOtherTableKeyParam = "othertablekey";
    public static final String IsTiledParam = "isTiled";

    /**
     * A do-nothing constructor - init does all the work.
     */
    public Data() {
        super();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        DcwRecordFile foo = (DcwRecordFile) request.getAttribute(DispatchServlet.RECORD_FILE_OBJ);
        if (foo == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            doWork(request, response, foo);
        } catch (FormatException fe) {
            response.getWriter().println("FormatException dealing with table: "
                    + fe);
        }
    }

    /**
     * Generates the heading used for each HTML table
     */
    protected TableRowElement generateHeader(HttpServletRequest req,
                                             HttpServletResponse resp,
                                             DcwColumnInfo[] dci) {
        TableRowElement thr = new TableRowElement();
        for (int i = 0; i < dci.length; i++) {
            DcwColumnInfo dc = dci[i];
            String colName = dc.getColumnName();
            if (dc.getValueDescriptionTable() == null) {
                thr.addElement(new TableHeaderElement(colName));
            } else {
                StringBuffer baseurl = new StringBuffer();
                baseurl.append(req.getContextPath());
                baseurl.append(req.getServletPath());
                baseurl.append(req.getPathInfo()).append("?");
                String show = req.getParameter(RowSelectParam);
                String vdtl = req.getParameter(VDTParam);
                if (show != null) {
                    baseurl.append(RowSelectParam).append("=");
                    baseurl.append(show).append("&");
                }
                baseurl.append(VDTParam).append("=");
                if (vdtl == null) {
                    vdtl = "";
                }
                boolean appendCol = true;
                boolean needSep = false;
                StringTokenizer st = new StringTokenizer(vdtl, ",");
                while (st.hasMoreTokens()) {
                    String sname = st.nextToken();
                    if (colName.equals(sname)) {
                        appendCol = false;
                    } else {
                        append(baseurl, sname, needSep);
                        needSep = true;
                    }
                }
                if (appendCol) {
                    append(baseurl, colName, needSep);
                }
                thr.addElement(THE(colName, baseurl.toString()));
            }
        }
        return thr;
    }

    public static StringBuffer append(StringBuffer base, String app,
                                      boolean needSep) {
        return (needSep ? base.append(",") : base).append(app);
    }

    public static final String ROWLIST_OBJECT = Data.class.getPackage()
            .getName()
            + ".rowlist";

    protected void doWork(HttpServletRequest request,
                          HttpServletResponse response, DcwRecordFile drf)
            throws FormatException, IOException {
        DcwColumnInfo dci[] = drf.getColumnInfo();

        int rowlist[] = (int[]) request.getAttribute(ROWLIST_OBJECT);

        ListElement rows = new ListElement();
        WrapElement table = new WrapElement("table", "BORDER=1 ALIGN=CENTER", rows);
        TableRowElement thr = generateHeader(request, response, dci);
        rows.addElement(thr);

        String row_show = request.getParameter(RowSelectParam);
        boolean printall = RowSelectAll.equals(row_show);
        boolean parseall = RowSelectTest.equals(row_show);
        boolean schemaonly = RowSelectNone.equals(row_show);

        String baseurl = request.getContextPath() + request.getServletPath()
                + request.getPathInfo();
        String all = baseurl + "?" + RowSelectParam + "=" + RowSelectAll;
        String none = baseurl + "?" + RowSelectParam + "=" + RowSelectNone;
        String some = baseurl;
        String test = baseurl + "?" + RowSelectParam + "=" + RowSelectTest;
        String qstr = request.getQueryString();
        if (rowlist != null) {
            qstr = null;
        }
        if (qstr == null) {
            qstr = VDTParam + "=ALL";
        }
        if (qstr.indexOf(VDTParam + "=") == -1) {
            qstr += "&" + VDTParam + "=ALL";
        }
        String vdtlookup = baseurl + "?" + qstr;

        response.getWriter().println("<H2>Table Data</H2>");
        String redisplay = "Redisplay " + buildHREF(response, all, "All")
                + "\r\n|" + buildHREF(response, none, "None") + "\r\n|"
                + buildHREF(response, some, "Some") + "\r\n|"
                + buildHREF(response, test, "Test") + "\r\n|"
                + buildHREF(response, vdtlookup, "All VDT Columns") + "\r\n";

        if (schemaonly) {
            response.getWriter().println("Data Omitted: " + redisplay);
            return;
        }

        RowMaker rm;
        String basepath = getRootDir(request);
        String joincol = request.getParameter(JoinColumnParam);
        String jointable = request.getParameter(JoinOtherTableParam);
        String jointablekey = request.getParameter(JoinOtherTableKeyParam);
        if ((joincol != null) && (jointable != null)) {
            String isTiledJoin = request.getParameter(IsTiledParam);
            boolean isTiled = Boolean.valueOf(isTiledJoin).booleanValue();
            if (Constants.ID.equals(jointablekey)) {
                rm = new JoinRowMaker(drf, joincol, jointable, isTiled);
            } else {
                rm = new ComplexJoinRowMaker(drf, joincol, jointable, jointablekey, isTiled);
            }
        } else if (drf.getTableName().equals(Constants.charVDTTableName)
                || drf.getTableName().equals(Constants.intVDTTableName)) {
            rm = new VDTRowMaker(request, response, basepath, drf);
        } else if (drf.getTableName().equals("fcs")) {
            rm = new FCSRowMaker(request, response, basepath, drf);
        } else if (request.getParameter(VDTParam) != null) {
            String subsetmarkup = request.getParameter(VDTParam);
            String[] ss = null;
            if (subsetmarkup != null) {
                StringTokenizer st = new StringTokenizer(subsetmarkup, ",", false);
                ss = new String[st.countTokens()];
                for (int i = 0; i < ss.length; i++) {
                    ss[i] = st.nextToken();
                    if ("ALL".equals(ss[i])) {
                        ss = null; // null array gets all VDT lookups
                        break;
                    }
                }
            }
            rm = new DetailRowMaker(drf, ss);
        } else if (drf.getTableName().endsWith(".fit")) {
            rm = new FITRowMaker(drf);
        } else if (drf.getTableName().endsWith(".cft")
                || drf.getTableName().endsWith(".cjt")) {
            rm = new ComplexFeatureJoinRowMaker(drf);
        } else {
            rm = new PlainRowMaker();
        }

        Iterator rowiter;
        if (rowlist != null) {
            rowiter = new TableSubsetRecordIterator(rowlist, drf);
        } else if (printall) {
            rowiter = new TableListIterator(drf);
        } else if (parseall) {
            rowiter = new TableTestParseIterator(drf);
        } else {
            rowiter = new TableSampleIterator(drf);
        }
        // response.getWriter().println("<hr>Tn = " +
        // drf.getTableName() +
        // "<hr>" + rm.getClass().getName() + " " +
        // rowiter.getClass().getName() + "<hr>");
        int rowcount = 0;
        while (rowiter.hasNext()) {
            if (rowcount++ >= 99) {
                response.getWriter().println(redisplay);
                table.generate(response.getWriter());
                rows = new ListElement();
                table = new WrapElement("table", "BORDER=1 ALIGN=CENTER", rows);
                rows.addElement(new WrapElement("CAPTION", new StringElement("table data")));
                rows.addElement(thr);

                rowcount = 0;
            }
            rows.addElement(rm.generateRow((List) rowiter.next()));
        }
        rm.close();
        response.getWriter().println(redisplay);
        table.generate(response.getWriter());
    }

    public ContextInfo getContextInfo() {
        return contextInfo;
    }

    public static String joinURL(HttpServletRequest request,
                                 HttpServletResponse response, int tag,
                                 String filename, String colname,
                                 String othertable, String othertablekey,
                                 boolean isTiled) {
        String pathInfo = request.getPathInfo();
        int index = pathInfo.lastIndexOf('/');
        String subpath = pathInfo.substring(0, index + 1);
        String url = request.getContextPath() + request.getServletPath()
                + subpath + filename + "?" + JoinColumnParam + "=" + colname
                + "&" + JoinOtherTableParam + "=" + othertable + "&"
                + JoinOtherTableKeyParam + "=" + othertablekey + "&"
                + IsTiledParam + "=" + isTiled;
        String value = "<A HREF=\"" + response.encodeURL(url) + "\">" + tag
                + "</A>\r\n";
        return value;
    }

    /**
     * An iterator that returns a subset of the table rows
     */
    private static class TableSampleIterator implements Iterator {
        private final int recordCount;
        private final int columnCount;
        private int curRow = 0;
        private final DcwRecordFile drf;

        public TableSampleIterator(DcwRecordFile drf) throws FormatException {
            this.drf = drf;
            recordCount = drf.getRecordCount();
            columnCount = drf.getColumnCount();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext() {
            return (curRow < recordCount);
        }

        public Object next() {
            if (curRow < 10) {
                curRow++;
            } else if (curRow == 10) {
                curRow = 100;
            } else {
                curRow += 100;
            }
            if (curRow > recordCount) {
                curRow = recordCount;
            }
            ArrayList al = new ArrayList(columnCount);
            try {
                if (!drf.getRow(al, curRow)) {
                    throw new NoSuchElementException();
                }
            } catch (FormatException fe) {
                throw new NoSuchElementException();
            }
            return al;
        }
    }

    /**
     * An iterator that returns a subset of the table rows, but parses
     * every record in the table.
     */
    private static class TableTestParseIterator implements Iterator {
        final ListIterator base;

        public TableTestParseIterator(DcwRecordFile drf) throws FormatException {
            base = new TableListIterator(drf);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext() {
            return base.hasNext();
        }

        public Object next() {
            int index;
            Object ret;
            do {
                index = base.nextIndex();
                ret = base.next();
            } while ((index > 10) && ((index % 100) != 0) && base.hasNext());
            return ret;
        }
    }

    /**
     * An iterator that will return every row in the table.
     */
    private static class TableListIterator implements java.util.ListIterator {
        private int curRow = 1;
        private final DcwRecordFile drf;
        private final int columnCount;
        private final int recordCount;

        public TableListIterator(DcwRecordFile drf) throws FormatException {
            this.drf = drf;
            columnCount = drf.getColumnCount();
            recordCount = drf.getRecordCount();
        }

        public void add(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean hasPrevious() {
            return (curRow > 1);
        }

        public boolean hasNext() {
            return (curRow <= recordCount);
        }

        private ArrayList getRow(int row) {
            ArrayList al = new ArrayList(columnCount);
            try {
                if (!drf.getRow(al, row)) {
                    throw new NoSuchElementException();
                }
            } catch (FormatException fe) {
                throw new NoSuchElementException();
            }
            return al;
        }

        public Object next() {
            return getRow(curRow++);
        }

        public Object previous() {
            return getRow(--curRow);
        }

        public int nextIndex() {
            return curRow;
        }

        public int previousIndex() {
            return (curRow - 1);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(Object o) {
            throw new UnsupportedOperationException();
        }
    }
}
