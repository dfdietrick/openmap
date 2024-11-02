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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/ContextInfo.java,v $
// $Revision: 1.8 $ $Date: 2005/08/11 20:39:16 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import com.bbn.openmap.layer.vpf.LibrarySelectionTable;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletContext;

/**
 * This class holds information retrieved from the ServletContext.
 */
public class ContextInfo {

	/**
	 * the name of the attribute where ContextInfo objects are in the ServletContext
	 */
	public static final String CONTEXT_INFO = ContextInfo.class.getPackage().getName() + ".contextInfo";
	/**
	 * the prefix used to configure VPF libraries in web.xml
	 */
	public static final String LIBRARY_PREFIX = ContextInfo.class.getPackage().getName() + ".vpf_library.";

	/**
	 * a map from library name to (String) library path
	 */
	private Map lib_pathmap;
	/**
	 * a map from library name to LibrarySelectionTable
	 */
	private Map lib_lstmap;

	/**
	 * A constructor - use getContextInfo to get one.
	 *
	 * @param context the ServletContext to use to initialize
	 * @see #getContextInfo
	 */
	private ContextInfo(ServletContext context) {
		lib_pathmap = createLibrariesMap(context);
		lib_lstmap = new HashMap();
	}

	/**
	 * Returns the ContextInfo object for the ServletContext.
	 *
	 * @param context the ServletContext to either get an existing ContextInfo from, or the context to use to initialize
	 * a new ContextInfo, if one doesn't already exist for the context.
	 */
	public static synchronized ContextInfo getContextInfo(ServletContext context) {
		ContextInfo ci = (ContextInfo) context.getAttribute(CONTEXT_INFO);
		if (ci == null) {
			ci = new ContextInfo(context);
			context.setAttribute(CONTEXT_INFO, ci);
		}
		return ci;
	}

	/**
	 * Grovels through the ServletContext initialization parameters and creates a map from library name to library path.
	 *
	 * @param context the context to grovel through
	 */
	private Map createLibrariesMap(ServletContext context) {
		HashMap library_map = new HashMap();
		for (Enumeration en = context.getInitParameterNames(); en.hasMoreElements();) {
			String s = (String) en.nextElement();
			if (s.startsWith(LIBRARY_PREFIX)) {
				String libname = s.substring(LIBRARY_PREFIX.length());
				String path = getPath(context, context.getInitParameter(s));
				if (path != null) {
					library_map.put(libname, path);
				} else {
					context.log("Excluding " + libname
							+ " from database list, can't resolve path");
				}

			}
		}
		return Collections.unmodifiableMap(library_map);
	}

	/**
	 * Try and find an absolute path from an init parameter
	 *
	 * @param context the context to use to resolve paths
	 * @param path the path to try and resolve
	 * @return an absolute path to a file (hopefully a directory) on the system, or null indicating the resolve failed
	 * to find anything useful.
	 */
	private String getPath(ServletContext context, String path) {
		// try to resolve as a relative path in the war file
		try {
			String p2 = context.getRealPath(path);
			if (p2 != null) {
				File f = new File(p2);
				if (f.exists()) {
					return p2;
				}
			}
		} catch (java.security.AccessControlException jsace) {
			// ignore, nothing to do but press on
		}
		// try to resolve as an absolute path on the system
		try {
			File f = new File(path);
			if (f.exists()) {
				return path;
			}
		} catch (java.security.AccessControlException jsace) {
			// ignore, nothing to do
		}
		return null;
	}

	/**
	 * Return a file object that the path resolves to. Performs some minimal checks to try and prevent an attacker from
	 * feeding in urls that cause the servlets to climb out of their sandbox. A better option is to use a servlet
	 * container with the ability to restrict servlet file access. For example, Apache Software Foundation's Tomcat 5
	 * Servlet/JSP Container running with the -security flag.
	 *
	 * @param pathInfo the path to resolve (expected to be of the form "/library_name_in_web_xml/path/to/file")
	 * @return a File if it could be resolved, null otherwise
	 */
	public String resolvePath(String pathInfo) {
		if ((pathInfo == null) || (pathInfo.indexOf("..") != -1)) { // don't
			// climb
			// out
			// of
			// sandbox
			return null;
		}
		int libStart = pathInfo.indexOf('/') + 1;
		int libEnd = pathInfo.indexOf('/', libStart);
		if (libEnd == -1) {
			libEnd = pathInfo.length();
		}
		String libname = pathInfo.substring(libStart, libEnd);
		String subpath = pathInfo.substring(libEnd);

		String lib_home = getPath(libname);
		if (lib_home == null) {
			return null;
		}

		return lib_home + "/" + subpath;
	}

	/**
	 * Returns a Set whose values are the (String) names of the configured libraries.
	 *
	 * @return a set of library names
	 */
	public Set keySet() {
		return new TreeSet(lib_pathmap.keySet());
	}

	/**
	 * Returns the path (or null) for the library
	 *
	 * @param libname the library name
	 * @return the path or null
	 */
	public String getPath(String libname) {
		return (String) lib_pathmap.get(libname);
	}

	/**
	 * Returns the LibrarySelectionTable (or null) for the library
	 *
	 * @param libname the library name
	 * @return the LST or null
	 */
	public LibrarySelectionTable getLST(String libname) {
		return (LibrarySelectionTable) lib_lstmap.get(libname);
	}

	/**
	 * Adds an LST for a library
	 *
	 * @param libname the library name
	 * @param lst the LibrarySelectionTable for libname
	 */
	public void putLST(String libname, LibrarySelectionTable lst) {
		lib_lstmap.put(libname, lst);
	}
}
