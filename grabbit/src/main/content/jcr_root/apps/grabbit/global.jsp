<%@include file="/libs/foundation/global.jsp"%>
<%@page session="false"
        import="com.day.cq.wcm.api.WCMMode" %>

<%
    // Additional fields

    WCMMode wcmMode = WCMMode.fromRequest(request);
    boolean isEditMode = (wcmMode == WCMMode.EDIT) || (wcmMode == WCMMode.DESIGN);
%>

<c:set var="isEditMode" value="<%=isEditMode%>"/>
