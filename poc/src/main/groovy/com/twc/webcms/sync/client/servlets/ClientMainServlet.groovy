package com.twc.webcms.sync.client.servlets

import com.twc.webcms.sync.client.services.ClientService
import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingSafeMethodsServlet

import javax.servlet.ServletException
import javax.servlet.http.HttpServletResponse

@CompileStatic
@Slf4j
@SlingServlet( methods = ['GET'], paths = ["/bin/twc/client/grab"] )
class ClientMainServlet extends SlingSafeMethodsServlet {

    @Reference(bind='setClientService')
    ClientService clientService

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException{
        final Collection<String> rootPaths = request.getParameter("paths").split(",").collect()
        response.contentType = "application/json"
        Collection<Long> jobExecutionIds = clientService.initiateGrab(rootPaths)
        response.status = HttpServletResponse.SC_OK
        response.writer.write(new JsonBuilder(jobExecutionIds).toString())
    }
}
