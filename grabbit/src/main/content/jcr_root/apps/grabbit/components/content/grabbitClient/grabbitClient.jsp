<%@include file="/libs/foundation/global.jsp"%>
<%
%><%@page session="false" %>
<%@ taglib prefix="twc" uri="http://www.twc.com/webcms/taglibs"%>
<twcable:defineObjects/>
<cq:includeClientLib js="apps.grabbit" />
<cq:includeClientLib css="apps.grabbit" />
<div>
    <form id="grabbit-form">
        <fieldset class="grabbit_client">
            <label for="linkPaths">Enter 1 or more new-line delimited paths :</label><br/>
            <textarea id="linkPaths" name="linkPaths" required="required" title="This field is required">
            </textarea><br/>
            <div class= "grab-content">
                <button type="submit" value="Start Replicate">Initiate</button>
            </div>
        </fieldset>
    </form>
    <div class="grab-status"/>
</div>
<div id="error">An error occurred while grabbing content. </div>
<div id="success">Content grab initiated</div>
<c:set var="uuid" value="${twcable:uuid()}"/>
<div id="${uuid}"></div>
<script>
    jQuery(document).ready(function() {
        jQuery('#${uuid}').closest('.grabbitClient').grabContent();
    });
</script>


