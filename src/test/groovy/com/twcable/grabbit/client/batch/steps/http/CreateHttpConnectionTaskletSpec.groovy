package com.twcable.grabbit.client.batch.steps.http

import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.client.batch.ClientBatchJobContext
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.StatusLine
import org.apache.http.auth.AuthScope
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.DefaultHttpClient
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.scope.context.StepContext
import org.springframework.batch.repeat.RepeatStatus
import spock.lang.Specification

import static javax.servlet.http.HttpServletResponse.*

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

class CreateHttpConnectionTaskletSpec extends Specification {

    def cleanup() {
        ClientBatchJobContext.inputStream = null
    }

    def getMockJobParameters() {
        return [
                (ClientBatchJob.SERVER_USERNAME)    : "username",
                (ClientBatchJob.SERVER_PASSWORD)    : "password",
                (ClientBatchJob.PATH)               : "/content/test",
                (ClientBatchJob.EXCLUDE_PATHS)      : "/exclude*/exclude/metoo",
                (ClientBatchJob.HOST)               : "localhost",
                (ClientBatchJob.PORT)               : "4503",
                (ClientBatchJob.CONTENT_AFTER_DATE) : "2008-09-22T13:57:31.2311892-04:00"
        ]
    }

    def "A successful connection to a Grabbit server results in the input stream being set on the ClientBatchJobContext"() {
        given:
        final chunkContext = Mock(ChunkContext) {
            getStepContext() >> Mock(StepContext) {
                getJobParameters() >> getMockJobParameters()
            }
        }
        final contribution = Mock(StepContribution) {
            0 * setExitStatus(ExitStatus.FAILED)
        }
        final inputStream = Mock(InputStream)
        final tasklet = Spy(CreateHttpConnectionTasklet) {
            getHttpClient() >> Mock(DefaultHttpClient) {
                execute((HttpGet)_, (HttpClientContext)_) >> Mock(CloseableHttpResponse) {
                    getStatusLine() >> Mock(StatusLine) {
                        getStatusCode() >> SC_OK
                    }
                    getEntity() >> Mock(HttpEntity) {
                        getContent() >> inputStream
                    }
                }
            }
        }

        when:
        final repeatStatus = tasklet.execute(contribution, chunkContext)

        then:
        repeatStatus == RepeatStatus.FINISHED
        ClientBatchJobContext.inputStream == inputStream
    }

    def "A failed connection to a Grabbit server results in an ExitStatus of FAILED"() {
        given:
        final chunkContext = Mock(ChunkContext) {
            getStepContext() >> Mock(StepContext) {
                getJobParameters() >> getMockJobParameters()
            }
        }
        final contribution = Mock(StepContribution) {
            1 * setExitStatus(ExitStatus.FAILED)
        }
        final inputStream = Mock(InputStream)
        final tasklet = Spy(CreateHttpConnectionTasklet) {
            getHttpClient() >> Mock(DefaultHttpClient) {
                execute((HttpGet)_, (HttpClientContext)_) >> Mock(CloseableHttpResponse) {
                    getStatusLine() >> Mock(StatusLine) {
                        getStatusCode() >> responseCode
                    }
                    getEntity() >> Mock(HttpEntity) {
                        getContent() >> inputStream
                    }
                }
            }
        }

        when:
        final repeatStatus = tasklet.execute(contribution, chunkContext)

        then:
        repeatStatus == RepeatStatus.FINISHED

        where:
        //Really, any HTTP response that is not SC_OK(200). Some likely potential candidates:
        responseCode << [SC_BAD_REQUEST, SC_UNAUTHORIZED, SC_FORBIDDEN, SC_NOT_FOUND, SC_CONFLICT, SC_BAD_GATEWAY]
    }

    def "getPreemptiveAuthContext() creates an authentication cache correctly"() {
        given:
        final CreateHttpConnectionTasklet tasklet = new CreateHttpConnectionTasklet()
        URI requestURI = new URI("http://localhost:4503/grabbit/content?path=%2Fcontent%2Ftest&after=2008-09-22T13%3A57%3A31.2311892-04%3A00")

        when:
        final HttpClientContext context = tasklet.getPreemptiveAuthContext(requestURI)

        then:
        context.getAuthCache() != null
        //We should be able to grab an AuthCache for the request credentials above
        context.getAuthCache().get(new HttpHost(requestURI.host, requestURI.port, requestURI.scheme)) != null
    }

    def "getHttpClient(username, password) provides an authentication ready client"() {
        given:
        final CreateHttpConnectionTasklet tasklet = new CreateHttpConnectionTasklet()

        when:
        final client = tasklet.getHttpClient("username", "password") as DefaultHttpClient
        final credentials = client.getCredentialsProvider().getCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT))

        then:
        credentials.userPrincipal.getName() == "username"
        credentials.password == "password"
    }

    def "buildURIForRequest() builds a Grabbit URI correctly"() {
        given:
        final jobParameters = getMockJobParameters()
        final CreateHttpConnectionTasklet tasklet = new CreateHttpConnectionTasklet()

        when:
        final URI uri = tasklet.buildURIForRequest(jobParameters)

        then:
        uri.toString() == "http://localhost:4503/grabbit/content?path=%252Fcontent%252Ftest&after=2008-09-22T13%253A57%253A31.2311892-04%253A00&excludePath=%252Fexclude&excludePath=%252Fexclude%252Fmetoo"
    }
}
