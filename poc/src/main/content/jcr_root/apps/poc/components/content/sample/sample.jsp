<%@ taglib prefix="ici" uri="http://www.icidigital.com/custom/taglibs" %>
<%@include file="/apps/icidigital/global.jsp"%>
<%@page session="false" %>

<c:set var="title" value="<%= properties.get("./title", "Default Value")%>" />
<h2>You entered : <ici:Hello name="${title}"/> </h2>
