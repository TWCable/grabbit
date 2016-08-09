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
import com.twcable.grabbit.resources.TransactionResource
import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingSafeMethodsServlet
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.context.ConfigurableApplicationContext

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * This servlet is used for querying Grabbit transactions.
 *
 * It is a handler for the {@link com.twcable.grabbit.resources.TransactionResource} resource.
 */
@Slf4j
@CompileStatic
@SlingServlet(methods = ['GET'], resourceTypes = ['twcable:grabbit/transaction'])
class GrabbitTransactionServlet extends SlingSafeMethodsServlet {

    @Reference(bind = 'setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext

    /**
     * @param request The request for querying a transaction.
     * @param response The response for the query. Will respond with an HTTP 200, and JSON status string if the transaction is found.
     * If the transaction isn't found or no transactionID is provided, will respond with an HTTP 400, and a description of what went wrong.
     */
    @Override
    void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        long transactionID
        try {
            String transactionIDString = request.resource.resourceMetadata[TransactionResource.TRANSACTION_ID_KEY]
            // Guard against the transactionID coming back as null or empty
            if(transactionIDString != null && !transactionIDString.empty) {
                //At this point we know we at least have something as the "transactionID." It may still not be a valid numeric ID
                transactionID = transactionIDString.toLong()
            }
        } catch(NumberFormatException ex) {
            log.warn "Bad request to receive transaction status for : ${request.pathInfo}. Transaction ID's must be numeric."
            response.setStatus(SC_BAD_REQUEST)
            response.writer.write("Request was made to get status of a transaction, but the transactionID provided for ${request.pathInfo} wasn't valid. Transaction ID's must be numeric.")
            return
        }
        if(transactionID) {
            final JobExplorer jobExplorer = configurableApplicationContext.getBean("clientJobExplorer", JobExplorer)
            final Collection<ClientJobStatus> jobStatus = ClientJobStatus.getAllForTransaction(jobExplorer, transactionID)
            if(jobStatus != null && !jobStatus.empty) {
                log.debug "Successful request to receive transaction status for : ${request.pathInfo}."
                response.setStatus(SC_OK)
                response.setContentType("application/json")
                response.writer.write(new JsonBuilder(jobStatus).toString())
                return
            }
            //We couldn't find a transaction matching the transactionID provided
            else {
                log.warn "Bad request to receive transaction status for : ${request.pathInfo}. The transaction was not found for the provided ID."
                response.setStatus(SC_BAD_REQUEST)
                response.writer.write("Request was made to get status of a transaction, but the transaction was not found for ${request.pathInfo}.")
                return
            }
        }
        log.warn "Bad request to receive transaction status for : ${request.pathInfo}. A transactionID was not provided."
        response.setStatus(SC_BAD_REQUEST)
        response.writer.write("Request was made to get status of a transaction, but a transactionID was not provided for ${request.pathInfo}.")
    }
}
