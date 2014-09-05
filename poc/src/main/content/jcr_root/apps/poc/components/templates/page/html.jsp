<%@include file="/apps/icidigital/global.jsp"%>
<%@page session="false"%>
<% 
// set doctype
    try {
        currentDesign.getDoctype(currentStyle).toRequest(request);
    } catch (Exception ex) {
        log.error("Design at "+ currentDesign.getPath() +" is missing cq:doctype property.");
        //default to HTML5
        com.day.cq.commons.Doctype.HTML_5.toRequest(request);
    }
%>
<cq:include script="content.jsp"/>
