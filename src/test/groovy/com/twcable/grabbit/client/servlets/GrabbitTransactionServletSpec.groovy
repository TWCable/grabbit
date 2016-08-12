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

import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.resources.TransactionResource
import com.twcable.grabbit.spring.batch.repository.GrabbitJobExecution
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceMetadata
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_OK

class GrabbitTransactionServletSpec extends Specification {

    def "A request for transaction status with an invalid non-numeric transactionId results in a bad request response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(TransactionResource.TRANSACTION_ID_KEY) >> "invalid"
                }
            }
            getPathInfo() >> "/grabbit/transaction/invalid"
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_BAD_REQUEST)
            getWriter() >> Mock(PrintWriter)
        }

        final transactionServlet = new GrabbitTransactionServlet()

        expect:
        transactionServlet.doGet(request, response)
    }

    def "A request for transaction status with an transactionId 'all' results in a OK response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(TransactionResource.TRANSACTION_ID_KEY) >> "all"
                }
            }
            getPathInfo() >> "/grabbit/transaction/all"
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_OK)
            getWriter() >> Mock(PrintWriter)
        }
        final applicationContext = Mock(ConfigurableApplicationContext) {
            getBean("clientJobExplorer", JobExplorer) >> Mock(JobExplorer)
        }

        final transactionServlet = new GrabbitTransactionServlet()
        transactionServlet.setConfigurableApplicationContext(applicationContext)

        expect:
        transactionServlet.doGet(request, response)
    }

    def "If no transactionId is provided for a transaction status request, we respond with a bad request response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(TransactionResource.TRANSACTION_ID_KEY) >> transactionId
                }
            }
            getPathInfo() >> "/grabbit/transaction"
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_BAD_REQUEST)
            getWriter() >> Mock(PrintWriter)
        }

        final transactionServlet = new GrabbitTransactionServlet()

        expect:
        transactionServlet.doGet(request, response)

        where:
        transactionId << ["", null]
    }

    def "If we query for a transaction that does not exist, we get a bad request response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(TransactionResource.TRANSACTION_ID_KEY) >> "988"
                }
            }
            getPathInfo() >> "/grabbit/transaction/988"
        }
        final response = Mock(SlingHttpServletResponse) {
            getWriter() >> Mock(PrintWriter)
            1 * setStatus(SC_BAD_REQUEST)
        }
        final jobInstance = Mock(JobInstance)
        final jobExecution = Mock(GrabbitJobExecution) {
            getId() >> 123L
            isRunning() >> true
            getTransactionID() >> 456L
            getJobParameters() >> Mock(JobParameters)
        }
        final applicationContext = Mock(ConfigurableApplicationContext) {
            getBean("clientJobExplorer", JobExplorer) >> Mock(JobExplorer) {
                //We only return a single job instance, which has a transactionID of "456" rather than our query for "988"
                getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE) >> [
                    jobInstance
                ]
                getJobExecutions(jobInstance) >> [
                    jobExecution
                ]
                getJobExecution(123L) >> jobExecution
            }
        }

        final transactionServlet = new GrabbitTransactionServlet()
        transactionServlet.setConfigurableApplicationContext(applicationContext)

        expect:
        transactionServlet.doGet(request, response)
    }

    def "Can get status of jobs with a transactionId"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(TransactionResource.TRANSACTION_ID_KEY) >> "456"
                }
            }
            getPathInfo() >> "/grabbit/transaction/456"
        }
        final response = Mock(SlingHttpServletResponse) {
            getWriter() >> Mock(PrintWriter)
            1 * setStatus(SC_OK)
            1 * setContentType("application/json")
        }
        final jobInstance = Mock(JobInstance)
        final jobExecution = Mock(GrabbitJobExecution) {
            getId() >> 123L
            isRunning() >> true
            getTransactionID() >> 456L
            getJobParameters() >> Mock(JobParameters)
        }
        final applicationContext = Mock(ConfigurableApplicationContext) {
            getBean("clientJobExplorer", JobExplorer) >> Mock(JobExplorer) {
                getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE) >> [
                    jobInstance
                ]
                getJobExecutions(jobInstance) >> [
                    jobExecution
                ]
                getJobExecution(123L) >> jobExecution
            }
        }

        final transactionServlet = new GrabbitTransactionServlet()
        transactionServlet.setConfigurableApplicationContext(applicationContext)

        expect:
        transactionServlet.doGet(request, response)
    }
}
