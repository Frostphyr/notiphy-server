<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
	<head>
		<meta charset="UTF-8">
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1">
		
		<link rel="icon" href="favicon.ico">
		<link rel="stylesheet" href="style.css">
		
		<title>Manager | Notiphy</title>
	</head>
	<body>
		<div class="content">
			<h1>Notiphy Manager</h1>
			<h2>Stats</h2>
			<table>
				<tr>
					<th>Name</th>
					<th>Current</th>
					<th>Highest</th>
				</tr>
				<c:forEach items="${trackers}" var="tracker">
					<tr>
						<td>${tracker.name}</td>
						<td>${tracker.current}</td>
						<td>${tracker.highest}</td>
					</tr>
				</c:forEach>
			</table>
			<h2>Log</h2>
			<textarea id="log" rows="10" readonly>${log}</textarea>
		</div>
	</body>
</html>