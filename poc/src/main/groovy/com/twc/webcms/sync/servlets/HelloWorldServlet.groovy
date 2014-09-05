package com.twc.webcms.sync.servlets

import com.twc.webcms.sync.services.HelloWorldService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.felix.scr.annotations.Reference
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet

import javax.servlet.ServletException

@CompileStatic
@Slf4j
@SlingServlet( methods = ['GET'], paths = ["/bin/icidigital/getWorld"] )
class HelloWorldServlet extends SlingAllMethodsServlet{

    @Reference
    HelloWorldService helloWorldService

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException{
        response.getWriter().write(helloWorldService.getMessage())
    }


}
