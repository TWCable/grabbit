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

package com.twcable.grabbit.client.servlets

import com.twcable.grabbit.ClientJobStatus
import com.twcable.grabbit.GrabbitConfiguration
import com.twcable.grabbit.GrabbitConfiguration.ConfigurationException
import com.twcable.grabbit.NonExistentJobException
import com.twcable.grabbit.client.services.ClientService
import com.twcable.grabbit.resources.JobResource
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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * This servlet is used to manage Grabbit jobs.
 *
 * <p>
 * This servlet effectively acts as the "front-end" to Grabbit. A Grabbit client may interact with this servlet to
 * start new jobs, and query status of jobs.
 * </p>
 *
 * This servlet acts as a handler for the {@link com.twcable.grabbit.resources.JobResource} resource.
 */
@Slf4j
@CompileStatic
@SlingServlet(methods = ['GET', 'PUT'], resourceTypes = ['twcable:grabbit/job'])
class GrabbitJobServlet extends SlingAllMethodsServlet {

    //A "special" meta-jobID that allows for the status of all jobs to be queried.
    static final String ALL_JOBS_ID = "all"

    @Reference(bind = 'setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext

    @Reference(bind = 'setClientService')
    ClientService clientService

    /**
     * Used for querying the status of a Grabbit job based on the jobID on the Grabbit resource URI (e.g /grabbit/job/123, where 123 is the jobID).
     * A special case for the jobID is ALL_JOBS_ID, to which this servlet will respond with the status of all jobs recorded.
     *
     * @param request The request to query jobs.
     * @param response Servlet will respond with a 200 if job status is successfully retrieved. A 400 (Bad Request) will be returned
     * if an invalid jobID is provided (non-numeric, or if non-numeric not the meta-jobID 'all'); a jobID is not provided; or
     * a job cannot be found with the provided jobID.
     */
    @Override
    void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        final jobExecutionId = request.resource.resourceMetadata[JobResource.JOB_EXECUTION_ID_KEY] as String
        if(jobExecutionId != null && !jobExecutionId.empty) {
            //If the jobID isn't numeric, and if it isn't 'all'; then it can't be a valid job ID
            if(!jobExecutionId.isNumber() && jobExecutionId != ALL_JOBS_ID) {
                log.warn "Bad request to receive job status for : ${request.pathInfo}. The job ID provided must be numeric, or it must be 'all'."
                response.setStatus(SC_BAD_REQUEST)
                response.writer.write("Request was made to get status of a job, but an invalid ID was provided for ${request.pathInfo}. Make sure the job ID provided is numeric, or 'all'.")
                return
            }
            if (request.pathInfo.endsWith("html")) {
                response.setStatus(SC_OK)
                response.writer.write("TODO : This will be replaced by a UI representation for the Jobs Status.")
                return
            } else {
                final String jsonResponse = getJsonResponseForJobStatus(jobExecutionId)
                if(jsonResponse) {
                    log.debug "Current Status : ${jsonResponse}"
                    response.setStatus(SC_OK)
                    response.contentType = "application/json"
                    response.writer.write(jsonResponse)
                    return
                }
                else {
                    log.warn "Request was made to get the status of a job, but a job could not be found for the ID provided for ${request.pathInfo}."
                    response.setStatus(SC_BAD_REQUEST)
                    response.writer.write("Request was made to get the status of a job, but a job could not be found for the ID provided for ${request.pathInfo}.")
                    return
                }
            }
        }
        log.warn "Bad request to receive job status for : ${request.pathInfo}. A job ID was not provided in the request."
        response.setStatus(SC_BAD_REQUEST)
        response.writer.write("Request was made to get the status of a job, but no job ID was provided for ${request.pathInfo}. Try again with ${request.pathInfo}/<Job-ID> or ${request.pathInfo}/all.")
    }

    /**
     * Used to create a new set of jobs from a Grabbit configuration file.
     *
     * @param request The request to create a new set of jobs.
     * @param response Will respond with a HTTP 200 if the configuration was accepted, and jobs started; A JSON array of jobIDs will be returned in the body, and a transactionId
     * in the response header, with the key "GRABBIT_TRANSACTION_ID." If no configuration is sent with the request, a bad request response will be returned. A bad request response
     * will also be returned if the configuration provided is rejected. See {@link com.twcable.grabbit.GrabbitConfiguration} to see why this might happen.
     */
    @Override
    protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        if(!request.inputStream) {
            response.setStatus(SC_BAD_REQUEST)
            response.writer.write("Sorry, we couldn't create your jobs. It looks like a Grabbit configuration file is missing from the request.")
            return
        }
        final configurationInput = IOUtils.toString(request.inputStream, request.characterEncoding)
        log.debug "Input: ${configurationInput}"

        //The Login of the user making this request.
        //This user will be used to connect to JCR
        //If the User is null, 'anonymous' will be used to connect to JCR
        final clientUsername = request.remoteUser
        final GrabbitConfiguration configuration
        try {
            configuration = GrabbitConfiguration.create(configurationInput)
        } catch(ConfigurationException ex) {
            log.warn "Bad configuration for request. ${ex.errors.values().join(',')}."
            response.status = SC_BAD_REQUEST
            response.writer.write("Bad configuration for request. More detail: \n\n ${new JsonBuilder(ex.errors).toString()}.")
            return
        }
        Collection<Long> jobExecutionIds = clientService.initiateGrab(configuration, clientUsername)
        log.info "Jobs started : ${jobExecutionIds}"
        log.info "Transaction started ${configuration.transactionID}"
        response.status = SC_OK
        response.contentType = "application/json"
        response.addHeader("GRABBIT_TRANSACTION_ID", String.valueOf(configuration.transactionID))
        response.writer.write(new JsonBuilder(jobExecutionIds).toString())
    }

    /**
     * Will return the status of a job from the {@link org.springframework.batch.core.explore.JobExplorer} used in JSON format.
     * @param jobId The jobID to get status.
     * @return Will return a JSON representation of a job's status, or null if the jobID is invalid (non-numeric, or if non-numeric not being 'all').
     */
    private String getJsonResponseForJobStatus(String jobId) {
        final JobExplorer jobExplorer = configurableApplicationContext.getBean("clientJobExplorer", JobExplorer)
        if (jobId.isNumber()) {
            //Returns Status for A Job
            try {
                final ClientJobStatus status = ClientJobStatus.get(jobExplorer, Long.valueOf(jobId))
                return new JsonBuilder(status).toString()
            }
            catch(NonExistentJobException ex) {
                log.warn "Queried for job with ID ${jobId} however none was found."
                log.debug ex.toString()
            }
        }
        else if (jobId == ALL_JOBS_ID) {
            //Returns Status for All Jobs Currently persisted in JobRepository.
            //They are returned in Descending order, with newest job being the first one.
            final Collection<ClientJobStatus> statuses = ClientJobStatus.getAll(jobExplorer)
            return new JsonBuilder(statuses).toString()
        }
        return null
    }
}
