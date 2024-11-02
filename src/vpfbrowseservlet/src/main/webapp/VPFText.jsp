<%@ page import="com.bbn.openmap.layer.vpf.*" %>
<%@ page session="false" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head><title>VPF Help</title></head>
<body>
<h1>VPF Help</h1>

This is help text.

Here we are.
<ul>
<li>Hostname: 
<jsp:useBean id="lst" class="com.bbn.openmap.servlet.vpfBrowse.LibraryBean">
    <% lst.setContext(application); %>
    <% lst.setResponse(response); %>
    <jsp:setProperty name="lst" property="path" param="db"/>
</jsp:useBean>
<li> DB: <jsp:getProperty name="lst" property="libName"/>
<% 
    LibrarySelectionTable lstt = lst.getLst();
    java.util.List<String> items = lstt.getLibraryNames();
    for (String item : items) {
%>
<li> library  <%= item %> 
<%
	}
%>
<li> dbname  <%= lstt.getDatabaseName() %> 
<li> description  <%= lstt.getDatabaseDescription() %> 
</ul>
</body>
</html>
