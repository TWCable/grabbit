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
package com.twcable.grabbit.client.batch.steps.http

import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.client.batch.ClientBatchJobContext
import okhttp3.HttpUrl
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.scope.context.StepContext
import org.springframework.batch.repeat.RepeatStatus
import spock.lang.Specification

import static com.twcable.grabbit.client.batch.steps.http.CreateHttpConnectionTasklet.Connection
import static javax.servlet.http.HttpServletResponse.SC_BAD_GATEWAY
import static javax.servlet.http.HttpServletResponse.SC_OK

class CreateHttpConnectionTaskletSpec extends Specification {


    def getMockJobParameters() {
        return [
                (ClientBatchJob.SERVER_USERNAME)    : "username",
                (ClientBatchJob.SERVER_PASSWORD)    : "password",
                (ClientBatchJob.PATH)               : "/content/test",
                (ClientBatchJob.EXCLUDE_PATHS)      : "/exclude*/exclude/metoo",
                (ClientBatchJob.SCHEME)             : "http",
                (ClientBatchJob.HOST)               : "localhost",
                (ClientBatchJob.PORT)               : "4503",
                (ClientBatchJob.CONTENT_AFTER_DATE) : "2008-09-22T13:57:31.2311892-04:00"
        ]
    }


    def "A successfully established connection sets the input stream on the ClientBatchContext"() {
        when:
        final ChunkContext chunkContext = Mock(ChunkContext) {
            getStepContext() >> Mock(StepContext) {
                getJobParameters() >> getMockJobParameters()
            }
        }
        final InputStream inputStream = Mock(InputStream)
        final CreateHttpConnectionTasklet tasklet = Spy(CreateHttpConnectionTasklet) {
            createConnection(getMockJobParameters()) >> Mock(Connection) {
                getStatus() >> SC_OK
                getInputStream() >> inputStream
            }
        }

        then:
        tasklet.execute(Mock(StepContribution), chunkContext) == RepeatStatus.FINISHED
        ClientBatchJobContext.inputStream != null
    }


    def "A failed connection logs out the network response, and sets status failed"() {
        when:
        final ChunkContext chunkContext = Mock(ChunkContext) {
            getStepContext() >> Mock(StepContext) {
                getJobParameters() >> getMockJobParameters()
            }
        }
        final StepContribution stepContribution = Mock(StepContribution) {
            1 * setExitStatus(ExitStatus.FAILED)
        }
        final InputStream inputStream = Mock(InputStream)
        final CreateHttpConnectionTasklet tasklet = Spy(CreateHttpConnectionTasklet) {
            createConnection(getMockJobParameters()) >> Mock(Connection) {
                getStatus() >> SC_BAD_GATEWAY
                getInputStream() >> inputStream
            }
        }

        then:
        tasklet.execute(stepContribution, chunkContext) == RepeatStatus.FINISHED
        ClientBatchJobContext.inputStream != null
    }


    def "buildURIForRequest() builds a Grabbit http URI correctly"() {
        given:
        final jobParameters = getMockJobParameters()
        final CreateHttpConnectionTasklet tasklet = new CreateHttpConnectionTasklet()

        when:
        final HttpUrl url = tasklet.getURLForRequest(jobParameters)

        then:
        url.toString() == "http://localhost:4503/grabbit/content?path=/content/test&after=2008-09-22T13:57:31.2311892-04:00&excludePath=/exclude&excludePath=/exclude/metoo"
    }


    def "buildURIForRequest() builds a Grabbit https URI correctly"() {
        given:
        final jobParameters = getMockJobParameters()
        jobParameters.put(ClientBatchJob.SCHEME, "https")
        final CreateHttpConnectionTasklet tasklet = new CreateHttpConnectionTasklet()

        when:
        final HttpUrl url = tasklet.getURLForRequest(jobParameters)

        then:
        url.toString() == "https://localhost:4503/grabbit/content?path=/content/test&after=2008-09-22T13:57:31.2311892-04:00&excludePath=/exclude&excludePath=/exclude/metoo"
    }
}
