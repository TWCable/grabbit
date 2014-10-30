package com.twc.grabbit.client.servlets

import com.twc.grabbit.client.GrabbitConfiguration
import com.twc.grabbit.client.services.ClientService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingAllMethodsServlet

import javax.servlet.ServletException
import javax.servlet.http.HttpServletResponse

@Slf4j
@SlingServlet( methods = ['POST'], paths = ["/bin/twc/client/grab"] )
class ClientMainServlet extends SlingAllMethodsServlet {

    @Reference(bind='setClientService')
    ClientService clientService

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException{
        if(!request.inputStream) throw new IllegalStateException("Input cannot be null or empty")

        final input = IOUtils.toString(request.inputStream, request.characterEncoding)
        log.debug "Input: ${input}"

        final GrabbitConfiguration configuration = GrabbitConfiguration.create(input)

        Collection<Long> jobExecutionIds = clientService.initiateGrab(configuration)

        response.contentType = "application/json"
        response.status = HttpServletResponse.SC_OK
        response.writer.write(new JsonBuilder(jobExecutionIds).toString())

    }

}
