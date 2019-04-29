<html>
<head><title>Botsing web application</title></head>
<body>
<h2>Error</h2>

<p>
An error has occurred:
<% if (session.getAttribute("errormessage") != null) {%>
	<%= session.getAttribute("errormessage")%>
	<% session.setAttribute("errormessage", null); %>
<% }%>
</p>
<a href="index.jsp">Home</a>
</body>
</html>
