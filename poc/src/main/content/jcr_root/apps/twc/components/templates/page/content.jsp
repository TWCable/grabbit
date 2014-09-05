<%@include file="/apps/twc/global.jsp"%>
<%@page session="false"%>
<%-- for sidekick --%>
<cq:include script="/libs/wcm/core/components/init/init.jsp"/>
<div class="container">

    <header>
        <cq:include script="header.jsp"/>
    </header> 
       <div id="main">
            <cq:include script="main.jsp" />
       </div>
    <footer>
    	<cq:include script="footer.jsp"/>
    </footer>
</div>

