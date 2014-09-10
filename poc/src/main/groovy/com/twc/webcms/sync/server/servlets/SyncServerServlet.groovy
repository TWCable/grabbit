package com.twc.webcms.sync.server.servlets

import com.twc.webcms.sync.server.services.SyncServerService
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
@SlingServlet( methods = ['GET'], paths = ["/bin/server/sync"] )
class SyncServerServlet extends SlingSafeMethodsServlet{

    @Reference(bind='setSyncServerService')
    SyncServerService syncServerService

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException{
        final String rootPath = request.queryString.split("=")[1]
        syncServerService.getProtosForRootPath(rootPath, response.outputStream)
    }


}
