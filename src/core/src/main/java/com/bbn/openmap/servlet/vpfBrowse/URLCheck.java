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
// $Source: /cvs/distapps/openmap/src/vpfservlet/WEB-INF/src/com/bbn/openmap/vpfservlet/URLCheck.java,v $
// $Revision: 1.4 $ $Date: 2005/08/11 20:39:15 $ $Author: dietrick $
// **********************************************************************
package com.bbn.openmap.servlet.vpfBrowse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Command line program to wander the vpfservlet pages, to make sure
 * all URLs generated are valid.
 */
public class URLCheck {
    public static Set check(String surl, PrintStream out) throws IOException {
        Set urls = new TreeSet();
        out.println("URL " + surl);
        Reader r;
        URL url;
        try {
            url = new URL(surl);
            r = new InputStreamReader(url.openStream());
        } catch (MalformedURLException mue) {
            out.println("   bad URL");
            return urls;
        }
        Set names = new HashSet();
        Set localrefs = new HashSet();
        StringBuffer sb = new StringBuffer();
        char buf[] = new char[8096];
        int len;
        while ((len = r.read(buf)) != -1) {
            sb.append(buf, 0, len);
            String str = sb.toString();
            int fromIx = 0;
            int gt;
            while ((gt = str.indexOf('>', fromIx)) != -1) {
                int lt = str.indexOf('<', fromIx);
                fromIx = gt + 1;
                char firstChar = str.charAt(lt + 1);
                if ((firstChar != 'A') && (firstChar != 'a')) {
                    continue;
                }
                String substr = str.substring(lt + 1, gt);
                String lsubstr = substr.toLowerCase();

                int hquote = lsubstr.indexOf(href);
                if (hquote != -1) {
                    hquote += href.length();
                    int lquote = substr.indexOf('"', hquote);
                    String rurl = substr.substring(hquote, lquote);
                    if (rurl.charAt(0) == '#') {
                        names.add(rurl.substring(1));
                    } else {
                        try {
                            urls.add(new URL(url, rurl).toExternalForm());
                        } catch (MalformedURLException mue) {
                            out.println("   MUE: " + mue.getMessage());
                        }
                    }
                } else {
                    int nquote = lsubstr.indexOf(name);
                    if (nquote != -1) {
                        nquote += name.length();
                        int lquote = substr.indexOf('"', nquote);
                        String n = substr.substring(nquote, lquote);
                        localrefs.add(n);
                    }
                }

            }
            sb.delete(0, fromIx);
        }
        for (Iterator i = localrefs.iterator(); i.hasNext();) {
            String localref = (String) i.next();
            if (!names.contains(localref)) {
                out.println("MISSING REF: " + localref);
            }
        }
        return urls;
    }
    final static String href = "href=\"";
    final static String name = "name=\"";

    public static Set workOn(Set master, Set urls, PrintStream out) {
        Set newurls = null;
        for (Iterator i = urls.iterator(); i.hasNext();) {
            String surl = (String) i.next();
            if (master.add(surl)) {
                try {
                    Set rets = check(surl, out);
                    if (newurls == null) {
                        newurls = rets;
                    } else {
                        newurls.addAll(rets);
                    }
                } catch (FileNotFoundException fnfe) {
                    out.println("Bogus URL: " + surl);
                } catch (IOException ioe) {
                    out.println("   " + surl + " " + ioe.getClass() + " "
                            + ioe.getMessage());
                }
            }
        }
        return newurls;
    }

    public static void main(String[] args) {
        Set master = new HashSet();
        Set workon = new HashSet();
        workon.addAll(Arrays.asList(args));
        do {
            workon = workOn(master, workon, System.out);
        } while (workon != null);
        System.out.println("Done.");
    }
}
