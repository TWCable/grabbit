/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twcable.grabbit.servlets

import com.twcable.grabbit.ClientJobStatus
import com.twcable.grabbit.GrabbitConfiguration
import com.twcable.grabbit.GrabbitConfiguration.ConfigurationException
import com.twcable.grabbit.client.services.ClientService
import com.twcable.grabbit.resources.JobResource
import com.twcable.grabbit.server.services.ServerService
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
@SlingServlet(methods = ['GET', 'PUT'], resourceTypes = ['twcable:grabbit/job'])
class GrabbitServlet extends SlingAllMethodsServlet {

    @Reference(bind = 'setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext

    @Reference(bind = 'setClientService')
    ClientService clientService

    @Reference(bind = 'setServerService')
    ServerService serverService


    @Override
    void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        final path = request.getParameter("path")
        final excludePaths = request.getParameterValues("excludePath")

        if (path) { //Server Get Request
            final decodedPath = URLDecoder.decode(path, "utf-8")
            final decodedExcludePaths = excludePaths.collect { String ep -> URLDecoder.decode(ep, 'UTF-8') }
            response.contentType = "application/octet-stream"

            final afterDateString = URLDecoder.decode(request.getParameter("after") ?: "", "utf-8")

            if(afterDateString) {
                log.info "Path : $decodedPath, Exclude Paths: $decodedExcludePaths, " +
                        "AfterDate String : $afterDateString. Will send only delta content"
            }

            //The Login of the user making this request.
            //This user will be used to connect to JCR
            //If the User is null, 'anonymous' will be used to connect to JCR
            final serverUsername = request.remoteUser
            serverService.getContentForRootPath(serverUsername, decodedPath, decodedExcludePaths ?: null,
                    afterDateString ?: null, response.outputStream)
        }
        else {
            final jobExecutionId = request.resource.resourceMetadata[JobResource.JOB_EXECUTION_ID] as String ?: ""
            if (request.pathInfo.endsWith("html")) {
                response.writer.write("TODO : This will be replaced by a UI representation for the Jobs Status")
            }
            else if (request.pathInfo.endsWith("json")) {
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
        if (!request.inputStream) throw new IllegalStateException("Input cannot be null or empty")

        final input = IOUtils.toString(request.inputStream, request.characterEncoding)
        log.debug "Input: ${input}"

        //The Login of the user making this request.
        //This user will be used to connect to JCR
        //If the User is null, 'anonymous' will be used to connect to JCR
        final clientUsername = request.remoteUser
        final GrabbitConfiguration configuration
        try {
            configuration = GrabbitConfiguration.create(input)
        } catch(ConfigurationException ex) {
            log.warn "Bad configuration for request. ${ex.errors.values().join(',')}"
            response.status = HttpServletResponse.SC_BAD_REQUEST
            response.contentType = "application/json"
            response.writer.write(new JsonBuilder(ex.errors).toString())
            return
        }
        Collection<Long> jobExecutionIds = clientService.initiateGrab(configuration, clientUsername)
        log.info "Jobs started : ${jobExecutionIds}"
        response.status = HttpServletResponse.SC_OK
        response.contentType = "application/json"
        response.writer.write(new JsonBuilder(jobExecutionIds).toString())
    }


    private String getJsonString(String jobId) {
        final JobExplorer jobExplorer = configurableApplicationContext.getBean("clientJobExplorer", JobExplorer)
        if (jobId.isNumber()) {
            //Returns Status for A Job
            final ClientJobStatus status = ClientJobStatus.get(jobExplorer, Long.valueOf(jobId))
            new JsonBuilder(status).toString()
        }
        else if (jobId == "all") {
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
