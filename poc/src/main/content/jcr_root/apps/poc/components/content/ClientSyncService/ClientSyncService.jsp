<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" %>
<%@ taglib prefix="twc" uri="http://www.twc.com/webcms/taglibs"%>
<twc:defineObjects/>
<cq:includeClientLib js="apps.poc" />
<cq:includeClientLib css="apps.poc" />
<div>
    <form id="sync-form">
        <fieldset class="sync_service">
            Enter comma separated paths:</br>
            <textarea id="linkPaths" name="linkPaths" required="required" title="This field is required"></textarea></br>
            <div  class= "grab-content">
                <button type="submit" value="Start Replicate" />
                <span>Submit</span>
            </div>
        </fieldset>
    </form>
</div>
<div id="error">An error occurred while grabbing content. </div>
<div id="success">Content pulled successfully</div>
<c:set var="uuid" value="${twc:uuid()}"/>
<div id="${uuid}"></div>
<script>
    jQuery(document).ready(function() {
        jQuery('#${uuid}').closest('.ClientSyncService').replicateContent();
    });
</script>


