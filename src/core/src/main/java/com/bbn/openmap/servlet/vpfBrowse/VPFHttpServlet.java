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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/VPFHttpServlet.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import com.bbn.openmap.util.html.TableHeaderElement;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A base class useful for servlets that use the VPF tools context object. This class also defines some utility methods
 * used in the package.
 */
public abstract class VPFHttpServlet extends HttpServlet {

	/**
	 * html doctype for HTML 4.0
	 */
	public static final String HTML_DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">";

	/**
	 * the context object used for config info
	 */
	protected ContextInfo contextInfo;

	/**
	 * A do-nothing constructor - init does all the work.
	 */
	public VPFHttpServlet() {
		super();
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		contextInfo = ContextInfo.getContextInfo(config.getServletContext());
	}

	/**
	 * Checks if a path refers to a file. If its not, reports an error.
	 *
	 * @param rootpath the path to check (can be null)
	 * @param pathInfo used in the error message if rootpath is null
	 * @param response used to send the error
	 * @return true if the file can be read, false otherwise
	 * @see HttpServletResponse#sendError
	 */
	public static boolean pathOkay(String rootpath, String pathInfo,
			HttpServletResponse response)
			throws IOException {
		if (rootpath == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, pathInfo
					+ " (invalid path)");
			return false;
		} else if (!new File(rootpath).canRead()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
					rootpath.toString() + " not found");
			return false;
		}
		return true;
	}

	/**
	 * Returns the HTML to reference the common stylesheet
	 *
	 * @return the stylesheet HTML
	 */
	public String getStylesheetHTML(HttpServletRequest request) {
		return ("<LINK REL =\"stylesheet\" TYPE=\"text/css\" HREF=\""
				+ request.getContextPath() + "/stylesheet.css\" TITLE=\"Style\">");
	}

	/**
	 * Returns a TableHeaderElement that contains a URL
	 *
	 * @param text the text for the reference
	 * @param url the URL for the reference
	 * @return a table cell containing a URL
	 */
	public TableHeaderElement THE(String text, String url) {
		return new TableHeaderElement(buildHREF(url, text));
	}

	/**
	 * Returns a TableHeaderElement that contains a URL
	 *
	 * @param text the text for the reference
	 * @param url the URL for the reference
	 * @param response the HttpServletResponse object used to encode the url
	 * @return a table cell containing a URL
	 */
	public TableHeaderElement THE(String text, String url,
			HttpServletResponse response) {
		return new TableHeaderElement(buildHREF(response, url, text));
	}

	/**
	 * Returns a string usable as an HTML HREF element
	 *
	 * @param tag the text for the reference
	 * @param url the URL for the reference
	 * @return a string containing an HTML HREF
	 */
	public static String buildHREF(String url, String tag) {
		return "<A HREF=\"" + url + "\">" + tag + "</A>";
	}

	/**
	 * Returns a string usable as an HTML HREF element
	 *
	 * @param tag the text for the reference
	 * @param url the URL for the reference
	 * @param response the HttpServletResponse object used to encode the url
	 * @return a string containing an HTML HREF
	 */
	public static String buildHREF(HttpServletResponse response, String url,
			String tag) {
		return buildHREF(response.encodeURL(url), tag);
	}

	/**
	 * Returns an HTML HREF based on the parameters. This method is equivelent to toURL(request, response,
	 * "/UnknownType", pathname, filename);
	 *
	 * @see #toURL
	 * @param request the request to use for context info
	 * @param response the response to use to encode the URL
	 * @param pathname the path of the file
	 * @param filename the name of the file
	 * @return a string HREF
	 */
	public static String fileURL(HttpServletRequest request,
			HttpServletResponse response, String pathname,
			String filename) {
		return toURL(request, response, "/UnknownType", pathname, filename);
	}

	/**
	 * Returns an HTML HREF based on the parameters.
	 *
	 * @param request the request to use for context info
	 * @param response the response to use to encode the URL
	 * @param pathname the path of the file
	 * @param filename the name of the file (may be null)
	 * @param servletName the servlet name to use in the URL
	 * @return a string HREF
	 */
	public static String toURL(HttpServletRequest request,
			HttpServletResponse response,
			String servletName, String pathname,
			String filename) {
		String value;
		if (filename == null) {
			value = "---";
		} else {
			String url = request.getContextPath() + servletName + pathname
					+ filename;
			value = "<A HREF=\"" + response.encodeURL(url) + "\">" + filename
					+ "</A>\r\n";

		}
		return value;
	}

	public static final String ROOT_PATHDIR = VPFHttpServlet.class.getName()
			+ ".rootPathDir";
	public static final String ROOT_PATH = VPFHttpServlet.class.getName()
			+ ".rootPath";

	public static void setRootDir(HttpServletRequest request, String path) {
		request.setAttribute(ROOT_PATHDIR, path);
	}

	public static String getRootDir(HttpServletRequest request) {
		return (String) request.getAttribute(ROOT_PATHDIR);
	}

	public static void setPathInfo(HttpServletRequest request, String path) {
		request.setAttribute(ROOT_PATH, path);
	}

	public static String getPathInfo(HttpServletRequest request) {
		return (String) request.getAttribute(ROOT_PATH);
	}

	protected static String setPathInfo(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		setPathInfo(request, pathInfo);
		if (pathInfo != null) {
			int index = pathInfo.lastIndexOf('/');
			String subpath = pathInfo.substring(0, index + 1);
			setRootDir(request, subpath);
		}
		return pathInfo;
	}
}
