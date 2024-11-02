// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source: /cvs/distapps/openmap/src/wmsservlet/WEB-INF/src/com/bbn/openmap/wmsservlet/HttpResponse.java,v $
// $RCSfile: HttpResponse.java,v $
// $Revision: 1.3 $
// $Date: 2008/02/20 01:41:08 $
// $Author: dietrick $
// 
// **********************************************************************
package com.bbn.openmap.servlet.wms;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import com.bbn.openmap.util.http.IHttpResponse;

/**
 */
public class HttpResponse implements IHttpResponse {

    protected HttpServletResponse httpResponse;

    /**
     * Initialize the input <code>Reader</code> and output <code>Writer</code>
     * and start the connection thread.
     *
     * @param response the response object
     */
    public HttpResponse(HttpServletResponse response) {
        this.httpResponse = response;
    }

    /**
     * Write a String response encoded as UTF-8 to the OutputStream.
     *
     * @param contentType the content type of the response.
     * @param response the string containing the response.
     */
    public void writeHttpResponse(String contentType, String response) throws IOException {
        writeHttpResponse(contentType, response.getBytes("UTF-8"));
    }

    /**
     * Write a byte[] response to the OutputStream.
     *
     * @param contentType the content type of the response.
     * @param response the byte array containing the response.
     */
    public void writeHttpResponse(String contentType, byte[] response) throws IOException {
        httpResponse.setContentType(contentType);
        httpResponse.setContentLength(response.length);
        OutputStream out = httpResponse.getOutputStream();
        out.write(response, 0, response.length);
        out.flush();
    }

}
