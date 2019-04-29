<%@page import="java.util.Properties"%>
<%@page import="eu.stamp_project.botsing.servlet.ServletUtils"%>
<html>
<head><title>Botsing web application</title></head>
<body>
<h2>Botsing Webapp</h2>

<%
// First look for local POM path in request
String pompath = request.getParameter("pompath");
if(pompath != null && pompath.trim().length() > 1) {
	// POM path found in request, update it in session
	session.setAttribute("pompath", pompath);
} else {
	// No POM path in request, look for it in session
	pompath = (String)session.getAttribute("pompath");
	if(pompath == null) {
		// No POM path in session: retrieve it from config, then store it in session
		Properties gitlabConfig = ServletUtils.getGitlabConfig(getServletContext());
		pompath = gitlabConfig.getProperty("local.defaultpom");
		if(pompath != null && pompath.trim().length() > 1) session.setAttribute("pompath", pompath);
	}
}
%>

<form method="POST" action="index.jsp">
Path to pom.xml on local disk (gitlab clone): <input type="text" name="pompath" size="65" value="<%=(pompath == null ? "" : pompath) %>">
<input type="submit" value="OK"/>
</form>
<hr/>

<% if(pompath != null && pompath.trim().length() > 1) { %>
<div>
<h2>Invoke Botsing from Gitlab issues</h2>
<p>
<a href="IssueServlet">Browse Gitlab issues</a><br/>
</p>
</div>
<hr/>

<div>
<h2>Invoke Botsing from log file with exception stack(s)</h2>
<form method="POST" action="LogFileServlet" enctype="multipart/form-data">
Select log file: <input type="file" name="logfile"/>
<input type="submit" value="OK"/>
<input type="reset" value="Cancel"/>
</form>
</div>
<hr/>

<div>
<h2>Invoke Botsing from raw text with exception stack(s)</h2>
Drop log with exception(s) here:<br/>
<form method="POST" action="BotsingServlet">
<input type="hidden" name="preprocess" value="y">
<textarea name="exception" cols="80" rows="16"></textarea>
<input type="submit" value="Process"/>
<input type="reset" value="Reset"/>
</form>
</div>

<% } %>

</body>
</html>
