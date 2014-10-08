package com.twc.webcms.sync.client.servlets

import com.twc.webcms.sync.client.services.ClientService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingSafeMethodsServlet

import javax.servlet.ServletException

@CompileStatic
@Slf4j
@SlingServlet( methods = ['GET'], paths = ["/bin/grabbit/client/pull"] )
class SyncClientServlet extends SlingSafeMethodsServlet {

    @Reference(bind='setClientService')
    ClientService clientService

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException{

        final Collection<String> rootPaths = request.getParameter("paths").split(",").collect();
        response.contentType = "application/octet-stream"
        clientService.doSync(rootPaths)
    }
}
