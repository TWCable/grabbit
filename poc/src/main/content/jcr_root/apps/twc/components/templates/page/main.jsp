<%@include file="/apps/twc/global.jsp"%>
<%@page session="false"%>
<div class="line wrapper">
    <div class="line">
        <div>
            <cq:include path="parsys" resourceType="foundation/components/parsys"/>
            <%-- Created a basic custom TagLib 'Hello' that prints out a 'simple string' --%>
            <ici:Hello name="twc"/>
        </div>
    </div>
</div>
