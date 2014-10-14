<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" %>
<%@ taglib prefix="twc" uri="http://www.twc.com/webcms/taglibs"%>
<twc:defineObjects/>
<cq:includeClientLib js="apps.poc" />
<cq:includeClientLib css="apps.poc" />
<div>
    <form id="sync-form">
        <fieldset class="sync_service">
            <label for="linkPaths">Enter 1 or more new-line delimited paths :</label><br/>
            <textarea id="linkPaths" name="linkPaths" required="required" title="This field is required">
/etc/tags
/content/residential-admin
/content/twc/en/checkout
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
<c:set var="uuid" value="${twc:uuid()}"/>
<div id="${uuid}"></div>
<script>
    jQuery(document).ready(function() {
        jQuery('#${uuid}').closest('.ClientSyncService').grabContent();
    });
</script>


