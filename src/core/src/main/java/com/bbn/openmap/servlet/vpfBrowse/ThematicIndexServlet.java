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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/ThematicIndexServlet.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
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
import com.bbn.openmap.layer.vpf.DcwThematicIndex;
import com.bbn.openmap.util.html.Element;
import com.bbn.openmap.util.html.HtmlListElement;
import com.bbn.openmap.util.html.ListElement;
import com.bbn.openmap.util.html.TableHeaderElement;
import com.bbn.openmap.util.html.TableRowElement;
import com.bbn.openmap.util.html.WrapElement;

/**
 * This servlet generates HTML for VPF files in thematic index format.
 */
public class ThematicIndexServlet
      extends VPFHttpServlet {

   /**
    * A do-nothing constructor - init does all the work.
    */
   public ThematicIndexServlet() {
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

      DcwThematicIndex ti;
      try {
         ti = new DcwThematicIndex(filePath, false);
      } catch (FormatException fe) {
         response.sendError(HttpServletResponse.SC_NOT_FOUND, fe.toString());
         return;
      }

      String valIndex = request.getParameter("valIndex");
      if (valIndex != null) {
         showTableIndexed(request, response, valIndex, ti);
      } else {
         showTableData(request, response, ti, filePath);
      }
      try {
         ti.close();
      } catch (FormatException fe) {
         // ignore
      }
   }

   protected void showTableData(HttpServletRequest request, HttpServletResponse response, DcwThematicIndex ti, String filePath)
         throws ServletException, IOException {

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      String title = "VPF Thematic Index for " + new File(filePath).getName();
      String basepath = getRootDir(request);
      out.println("<HEAD><TITLE>" + title + "</TITLE></HEAD>");
      out.println(getStylesheetHTML(request));
      out.println("<BODY><H1 CLASS=TableHeadingColor>" + title + "</H1>");

      out.println("<H3 CLASS=TableSubheadingColor>General Thematic Index Information</H3>");
      HtmlListElement list = new HtmlListElement();
      list.addElement("Number of Codes: " + ti.getNumberOfCodes());
      list.addElement("Number of Rows: " + ti.getNumberOfRows());
      list.addElement("Type Of Index: " + ti.getTypeOfIndex());
      list.addElement("Field Type of Index: " + ti.getFieldTypeOfIndex());
      list.addElement("Number of Data Elements: " + ti.getNumberOfDataElements());
      list.addElement("Data Type Specifier: " + ti.getDataTypeSpecifier());
      list.addElement("Table Indexed: " + fileURL(request, response, basepath, ti.getTableIndexed()));
      list.addElement("Column Indexed: " + ti.getColumnIndexed());
      list.addElement("Fields Sorted: " + ti.getSorted());
      list.generate(out);

      out.println("<H3 CLASS=TableSubheadingColor>Thematic Index Data</H3>");
      Object[] values = ti.getValueIndexes();
      ListElement rows = null;
      Element table = null;
      TableRowElement th = new TableRowElement();
      th.addElement(new TableHeaderElement("CLASS=NavBarCell2", "Index Value"));
      th.addElement(new TableHeaderElement("Count"));
      th.addElement(new TableHeaderElement("Rows..."));
      String valStr = "<A HREF=\"" + request.getContextPath() + request.getServletPath() + getPathInfo(request) + "?valIndex=";
      for (int i = 0; i < values.length; i++) {
         if ((i % 50) == 0) {
            if (table != null) {
               table.generate(out);
            }
            rows = new ListElement();
            table = new WrapElement("table", "BORDER=1", rows);
            rows.addElement(th);
         }
         TableRowElement tr = new TableRowElement();
         if (rows != null) {
            rows.addElement(tr);
         }
         tr.addElement(valStr + values[i] + "\">" + values[i] + "</A>");
         try {
            int[] intvals = ti.get(values[i]);
            tr.addElement(Integer.toString(intvals.length));
            StringBuffer sb = new StringBuffer();
            sb.append(intvals[0]);
            for (int j = 1; j < intvals.length; j++) {
               sb.append(", ").append(intvals[j]);
            }
            tr.addElement(sb.toString());
         } catch (FormatException fe) {
            tr.addElement(fe.toString());
         }
      }
      if (table != null) {
         table.generate(out);
      }
   }

   protected void showTableIndexed(HttpServletRequest request, HttpServletResponse response, String valIndex, DcwThematicIndex ti)
         throws IOException, ServletException {
      Object val = null;
      switch (ti.getFieldTypeOfIndex()) {
         case 'I':
            val = Integer.valueOf(valIndex);
            break;
         case 'S':
            val = Short.valueOf(valIndex);
            break;
         case 'T':
            val = valIndex;
            break;
      }
      try {
         int[] vals = ti.get(val);
         request.setAttribute(Data.ROWLIST_OBJECT, vals);
      } catch (FormatException fe) {
         response.sendError(HttpServletResponse.SC_NOT_FOUND, fe.toString());
      }
      String pi = request.getPathInfo();
      int lin = pi.lastIndexOf('/') + 1;
      RequestDispatcher rd = request.getRequestDispatcher("/UnknownType" + pi.substring(0, lin) + ti.getTableIndexed());
      rd.forward(request, response);
   }

}
