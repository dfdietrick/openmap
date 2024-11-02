<%@ page session="false" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<jsp:useBean id="table" class="com.bbn.openmap.servlet.vpfBrowse.VPFTable" scope="page"/>
<jsp:useBean id="table2" class="com.bbn.openmap.servlet.vpfBrowse.VPFTable" scope="page"/>
<jsp:useBean id="table3" class="com.bbn.openmap.servlet.vpfBrowse.VPFTable" scope="request"/>
	
<% table.setFile(request.getParameter("table")); %>
<jsp:setProperty name="table2" property="file" value='<%= request.getParameter("table")%>'/>
<jsp:setProperty name="table3" property="file" param="table"/>

<html>
<head><title>VPF Table <%= table.getTablename() %> </title></head>
<body>
<h1>VPF Table <jsp:getProperty name="table2" property="tablename"/> Help</h1>

This is help text rambling on about <jsp:getProperty name="table3"
property="tablename"/>, whose description is <jsp:getProperty
name="table3" property="description"/>.
</body>
</html>
