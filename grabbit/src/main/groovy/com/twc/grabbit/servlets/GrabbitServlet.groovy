package com.twc.grabbit.servlets

import com.twc.grabbit.ClientJobStatus
import com.twc.grabbit.GrabbitConfiguration
import com.twc.grabbit.client.services.ClientService
import com.twc.grabbit.resources.JobResource
import com.twc.grabbit.server.services.ServerService
import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.io.IOUtils
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingAllMethodsServlet
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.context.ConfigurableApplicationContext

import javax.servlet.http.HttpServletResponse

@Slf4j
@CompileStatic
@SlingServlet( methods = [ 'GET', 'PUT' ], resourceTypes = [ 'twc:grabbit/job' ] )
class GrabbitServlet extends SlingAllMethodsServlet {

    @Reference(bind='setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext

    @Reference(bind='setClientService')
    ClientService clientService

    @Reference(bind='setServerService')
    ServerService serverService

    @Override
    void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        final String path = request.getParameter("path")

        if(path) { //Server Get Request
            response.contentType = "application/octet-stream"
            serverService.getContentForRootPath(path, response.outputStream)
        }
        else {
            final jobExecutionId = request.resource.resourceMetadata[JobResource.JOB_EXECUTION_ID] as String ?: ""
            if(request.pathInfo.endsWith("html")) {
                response.writer.write("TODO : This will be replaced by a UI representation for the Jobs Status")
            }
            else if(request.pathInfo.endsWith("json")) {
                final String jsonString = getJsonString(jobExecutionId)
                log.debug "Current Status : ${jsonString}"
                response.contentType = "application/json"
                response.status = HttpServletResponse.SC_OK
                response.writer.write(jsonString)
            }
            else {
                log.error "Illegal Selector for current request : ${request.pathInfo}"
            }
        }
    }

    @Override
    protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        if(!request.inputStream) throw new IllegalStateException("Input cannot be null or empty")

        final input = IOUtils.toString(request.inputStream, request.characterEncoding)
        log.debug "Input: ${input}"

        final GrabbitConfiguration configuration = GrabbitConfiguration.create(input)
        Collection<Long> jobExecutionIds = clientService.initiateGrab(configuration)
        log.info "Jobs started : ${jobExecutionIds}"
        response.status = HttpServletResponse.SC_OK
        response.contentType = "application/json"
        response.writer.write(new JsonBuilder(jobExecutionIds).toString())
    }

    private String getJsonString(String jobId) {
        final JobExplorer jobExplorer = configurableApplicationContext.getBean("clientJobExplorer", JobExplorer)
        if(jobId.isNumber()) {
            //Returns Status for A Job
            final ClientJobStatus status = ClientJobStatus.get(jobExplorer, Long.valueOf(jobId))
            new JsonBuilder(status).toString()
        }
        else if (jobId == "all" ){
            //Returns Status for All Jobs Currently persisted in JobRepository
            //They are returned in Descending order, with newest job being the first one
            final Collection<ClientJobStatus> statuses = ClientJobStatus.getAll(jobExplorer)
            new JsonBuilder(statuses).toString()
        }
        else {
            throw new IllegalArgumentException("Invalid jobInstanceId : ${jobId}")
        }
    }
}
