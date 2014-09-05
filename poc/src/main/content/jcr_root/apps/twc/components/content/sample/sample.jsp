<%@ taglib prefix="ici" uri="http://www.twc.com/custom/taglibs" %>
<%@include file="/apps/twc/global.jsp"%>
<%@page session="false" %>

<c:set var="title" value="<%= properties.get("./title", "Default Value")%>" />
<h2>You entered : <ici:Hello name="${title}"/> </h2>
