<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1">
    
    <link rel="icon" href="favicon.ico">
    <link rel="stylesheet" href="style.css">
    
    <title>Status | Notiphy</title>
  </head>
  <body>
    <div class="content">
      <h1>Notiphy Status</h1>
      <p class="text-center">Memory ${memory}/${totalMemory}</p>
      <table class="horizontal-middle">
        <tr>
          <th>Name</th>
          <th>Status</th>
        </tr>
        <c:forEach items="${statuses}" var="status">
          <tr>
            <td>${status.name}</td>
            <td>${status.status}</td>
          </tr>
        </c:forEach>
      </table>
      <div class="divider margin-y-large"></div>
      <form class="text-center" action="?" method="get">
      	<input type="email" name="email" placeholder="Email" size="30">
      	<input class="button" type="submit" value="Search">
      </form>
      <c:if test="${not empty userError}">
        <p class="text-center">${userError}</p>
      </c:if>
      <c:if test="${not empty userEmail}">
        <p class="text-center">Email: ${userEmail}</p>
        <p class="text-center">Providers: ${userProviders}</p>
        <p class="text-center">Token: ${userToken}</p>
        <textarea class="unresizable width-100" rows="10" readonly>${userEntries}</textarea>
      </c:if>
    </div>
  </body>
</html>