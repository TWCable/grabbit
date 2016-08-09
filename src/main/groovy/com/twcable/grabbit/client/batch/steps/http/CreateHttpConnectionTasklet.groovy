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
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.AuthCache
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.DefaultHttpClient
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

import javax.annotation.Nonnull

import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * This tasklet opens an HTTP session for the current job, and places the input stream on the {@link ClientBatchJobContext}
 */
@CompileStatic
@Slf4j
class CreateHttpConnectionTasklet implements Tasklet {

    /**
     * This hook is called by Spring when it is time to create a connection from a Grabbit client, to a Grabbit server
     * to retrieve a stream of content.
     * @param contribution The StepContribution passed in by Spring.
     * @param chunkContext The ChunkContext passed in by Spring.
     * @return Always a {@link RepeatStatus} of FINISHED
     */
    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        final Map jobParameters = chunkContext.stepContext.jobParameters

        final URI requestURI = buildURIForRequest(jobParameters)

        final String username = jobParameters.get(ClientBatchJob.SERVER_USERNAME)
        final String password = jobParameters.get(ClientBatchJob.SERVER_PASSWORD)
        final client = getHttpClient(username, password)

        //This allows us to authenticate against the Grabbit server preemptively
        final httpClientContext = getPreemptiveAuthContext(requestURI)
        final get = new HttpGet(requestURI)
        final response = client.execute(get, httpClientContext)

        final int statusCode = response.getStatusLine().statusCode
        if(statusCode != SC_OK) {
            log.warn "Received a status of ${statusCode} when attempting to create a connection to ${jobParameters.get(ClientBatchJob.HOST)}. Bailing out. See debug log for more details."
            log.debug "Request to start a connection with ${jobParameters.get(ClientBatchJob.HOST)} resulted in: ${response.entity}."
            contribution.setExitStatus(ExitStatus.FAILED)
            return RepeatStatus.FINISHED
        }
        HttpEntity responseEntity = response.entity
        ClientBatchJobContext.setInputStream(responseEntity.content)
        return RepeatStatus.FINISHED
    }

    /**
     * Takes Grabbit job parameters and builds a URI for streaming content from server.
     * @param jobParameters A Grabbit job's parameters. No validation for parameters at this point.
     * @return {@link URI} for pulling a stream of Grabbit content.
     */
    private URI buildURIForRequest(@Nonnull final Map jobParameters) {
        final String path = jobParameters.get(ClientBatchJob.PATH)
        final String excludePathParam = jobParameters.get(ClientBatchJob.EXCLUDE_PATHS)
        final excludePaths = (excludePathParam != null && !excludePathParam.isEmpty() ? excludePathParam.split(/\*/) : Collections.EMPTY_LIST) as Collection<String>
        final String host = jobParameters.get(ClientBatchJob.HOST)
        final String port = jobParameters.get(ClientBatchJob.PORT)
        final String contentAfterDate = jobParameters.get(ClientBatchJob.CONTENT_AFTER_DATE) ?: ""

        final String encodedContentAfterDate = URLEncoder.encode(contentAfterDate, 'utf-8')
        final String encodedPath = URLEncoder.encode(path, 'utf-8')

        URIBuilder uriBuilder = new URIBuilder(scheme: "http", host: host, port: port as Integer, path: "/grabbit/content")
        uriBuilder.addParameter("path", encodedPath)
        uriBuilder.addParameter("after", encodedContentAfterDate)
        for(String excludePath : excludePaths) {
            uriBuilder.addParameter("excludePath", URLEncoder.encode(excludePath, 'UTF-8'))
        }
        return uriBuilder.build()
    }

    /**
     * Provides an HTTP client, complete with authentication credentials
     * @param username to authenticate the request
     * @param password to authenticate the request
     * @return a {@link HttpClient} instance for making Grabbit requests
     */
     HttpClient getHttpClient(final String username, final String password) {
        final client = getHttpClient() as DefaultHttpClient

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider()
        credentialsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password)
        )
        client.setCredentialsProvider(credentialsProvider)
        return client
    }

    /**
     * Provides a bare-bones HTTP client
     * @return a {@link HttpClient} instance for making Grabbit requests
     */
     HttpClient getHttpClient() {
        return new DefaultHttpClient()
    }

    /**
     * Provides a HttpClientContext for preemptive authentication
     * @param uri The Grabbit request URI to associate with the preemptive authentication
     * @return {@link HttpClientContext} for preemptive authentication
     */
    private HttpClientContext getPreemptiveAuthContext(@Nonnull final URI uri) {
        // Setup preemptive Authentication
        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache()
        HttpHost host = new HttpHost(uri.host, uri.port, uri.scheme)
        authCache.put(host, new BasicScheme())
        // Add AuthCache to the execution context
        HttpClientContext context = HttpClientContext.create()
        context.setAuthCache(authCache)
        return context
    }
}
