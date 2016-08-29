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
package com.twcable.grabbit.client

import com.twcable.grabbit.ClientJobStatus
import com.twcable.grabbit.NonExistentJobException
import com.twcable.grabbit.client.services.ClientService
import com.twcable.grabbit.client.servlets.GrabbitJobServlet
import com.twcable.grabbit.resources.JobResource
import com.twcable.grabbit.spring.batch.repository.GrabbitJobExecution
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceMetadata
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobExecutionNotRunningException
import org.springframework.batch.core.launch.NoSuchJobException
import org.springframework.batch.core.launch.support.SimpleJobOperator
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import javax.annotation.Nonnull
import javax.servlet.ServletInputStream

import static com.twcable.grabbit.client.servlets.GrabbitJobServlet.ALL_JOBS_ID
import static javax.servlet.http.HttpServletResponse.*

@Subject(GrabbitJobServlet)
class GrabbitJobServletSpec extends Specification {

    def "When polling for job status, and an '.html' extension is provided in the path; we return a stub SC_OK"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(JobResource.JOB_EXECUTION_ID_KEY) >> "123"
                }
            }
            getPathInfo() >> "/grabbit/job/123.html"
        }
        final responseWriter = Mock(PrintWriter)
        final response = Mock(SlingHttpServletResponse) {
            getWriter() >> responseWriter
        }

        when:
        new GrabbitJobServlet().doGet(request, response)

        then:
        1 * response.setStatus(SC_OK)
        1 * responseWriter.write("TODO : This will be replaced by a UI representation for the Jobs Status.")
    }

    @Unroll
    def "Polling for job status with a valid job ID #jobId works as expected"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(JobResource.JOB_EXECUTION_ID_KEY) >> jobId
                }
            }
            getPathInfo() >> "/grabbit/job/${jobId}"
        }
        final response = Mock(SlingHttpServletResponse) {
            getWriter() >> Mock(PrintWriter)
            1 * setStatus(SC_OK)
            1 * setContentType("application/json")
        }
        final applicationContext = Mock(ConfigurableApplicationContext) {
            getBean("clientJobExplorer", JobExplorer) >> Mock(JobExplorer) {
                getJobExecution(_) >> Mock(GrabbitJobExecution) {
                    isRunning() >> true
                    getJobParameters() >> Mock(JobParameters)
                }
            }
        }

        expect:
        final grabbitClientServlet = new GrabbitJobServlet()
        grabbitClientServlet.setConfigurableApplicationContext(applicationContext)
        grabbitClientServlet.doGet(request, response)


        where:
        //A jobID is expected to be ALL_JOBS_ID or a number. See GrabbitJobServlet for more info on the ALL_JOBS_ID.
        jobId << [ALL_JOBS_ID, "123"]
    }

    def "Polling for job status with a missing jobId receives a bad request response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(JobResource.JOB_EXECUTION_ID_KEY) >> ""
                }
            }
            getPathInfo() >> "/grabbit/job"
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_BAD_REQUEST)
            getWriter() >> Mock(PrintWriter)
        }

        expect:
        new GrabbitJobServlet().doGet(request, response)
    }

    def "Polling for job status with an invalid jobID (non-numeric, not 'all') receives a bad request response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(JobResource.JOB_EXECUTION_ID_KEY) >> "bad"
                }
            }
            getPathInfo() >> "/grabbit/bad"
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_BAD_REQUEST)
            getWriter() >> Mock(PrintWriter)
        }

        expect:
        new GrabbitJobServlet().doGet(request, response)
    }

    def "Polling for job status with a jobID for a job that does not exist receives a bad request response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
                getResourceMetadata() >> Mock(ResourceMetadata) {
                    get(JobResource.JOB_EXECUTION_ID_KEY) >> "123"
                }
            }
            getPathInfo() >> "/grabbit/job/123"
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_BAD_REQUEST)
            getWriter() >> Mock(PrintWriter)
        }
        final applicationContext = Mock(ConfigurableApplicationContext) {
            getBean("clientJobExplorer", JobExplorer) >> Mock(JobExplorer)
        }
        ClientJobStatus.metaClass.get = {JobExplorer explorer, Long value ->
            throw new NonExistentJobException()
        }

        final GrabbitJobServlet servlet = new GrabbitJobServlet()
        servlet.setConfigurableApplicationContext(applicationContext)

        expect:
        servlet.doGet(request, response)
    }

    def "When creating new jobs, if no configuration is provided, a bad request is given"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getInputStream() >> null
        }
        final response = Mock(SlingHttpServletResponse) {
            getWriter() >> Mock(PrintWriter) {
                1 * write("Sorry, we couldn't create your jobs. It looks like a Grabbit configuration file is missing from the request.")
            }
            1 * setStatus(SC_BAD_REQUEST)
        }

        expect:
        new GrabbitJobServlet().doPut(request, response)
    }

    def "Can create a new job successfully from configuration"() {
        given:
        File configFile = new File(this.class.getResource("test_config.yaml").getFile())
        final inputStream = new StubServletInputStream(configFile)
        final clientService = Mock(ClientService) {
            initiateGrab(_, _) >> [123L]
        }
        final request = Mock(SlingHttpServletRequest) {
            getInputStream() >> inputStream
            getCharacterEncoding() >> "UTF-8"
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_OK)
            1 * setContentType("application/json")
            getWriter() >> Mock(PrintWriter) {
                1 * write("[123]")
            }
            1 * addHeader("GRABBIT_TRANSACTION_ID", (String)_)
        }

        when:
        final grabbitClientServlet = new GrabbitJobServlet()
        grabbitClientServlet.setClientService(clientService)

        then:
        grabbitClientServlet.doPut(request, response)
    }

    def "A 400 (Bad Request) is returned if the configuration provided is malformed or incorrect"() {
        given:
        SlingHttpServletRequest request = Mock(SlingHttpServletRequest)
        SlingHttpServletResponse response = Mock(SlingHttpServletResponse)
        GrabbitJobServlet servlet = new GrabbitJobServlet()

        when:
        request.getInputStream() >> inputStream
        request.getCharacterEncoding() >> "UTF-8"
        request.getRemoteUser() >> "admin"

        response.getWriter() >> Mock(PrintWriter)

        servlet.doPut(request, response)

        then:
        1 * response.setStatus(SC_BAD_REQUEST)

        where:
        inputStream << [new StubServletInputStream(" "), new StubServletInputStream("foo: 'foo'")]
        //One causes SnakeYAML to produce a null config map, and the other does not pass our validations (missing values)
    }

    def "Stop job response check with differnt set of jobId parameter"() {
        given:
        SlingHttpServletRequest request = Mock(SlingHttpServletRequest) {
            getParameter("jobId") >> inputJobId
        }
        SlingHttpServletResponse response = Mock(SlingHttpServletResponse) {
            getWriter() >> Mock(PrintWriter)
        }
        GrabbitJobServlet servlet = new GrabbitJobServlet()
        final applicationContext = Mock(ConfigurableApplicationContext) {
            getBean(SimpleJobOperator) >> Mock(SimpleJobOperator) {
                stop(123L) >> true
                stop(222L) >> {throw new NoSuchJobException("Job does not exist")}
                stop(333L) >> {throw new JobExecutionNotRunningException("Job not running")}
            }
        }
        servlet.setConfigurableApplicationContext(applicationContext)

        when:
        servlet.doDelete(request, response)

        then:
        1 * response.setStatus(expectedResponse)

        where:
        inputJobId  | expectedResponse
        123L        | SC_OK
        222L        | SC_NOT_FOUND
        333L        | SC_NOT_FOUND
        "aaa"       | SC_BAD_REQUEST
        ""          | SC_BAD_REQUEST
    }

    class StubServletInputStream extends ServletInputStream {


        private final int byte_length
        private final byte[] bytes
        private int byte_index = 0

        StubServletInputStream(@Nonnull final String data) {
            bytes = data as byte[]
            byte_length = bytes.length
        }

        StubServletInputStream(@Nonnull final File fromFile) {
            bytes = fromFile.readBytes()
            byte_length = bytes.length
        }

        @Override
        int read() throws IOException {
            if (byte_index <= byte_length - 1) {
                final thisByte = bytes[byte_index] as int
                byte_index++
                return thisByte
            }
            return -1
        }
    }

}
