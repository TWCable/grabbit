package com.twcable.grabbit.client.batch.steps.http

import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.client.batch.ClientBatchJobContext
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
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
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

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

/**
 * This tasklet opens an HTTP session for the current job, and places the input stream on the {@link ClientBatchJobContext}
 */
class CreateHttpConnectionTasklet implements Tasklet {

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        HttpResponse response = doRequest(chunkContext.stepContext.jobParameters)
        HttpEntity responseEntity = response.entity
        ClientBatchJobContext.setInputStream(responseEntity.content)
        return RepeatStatus.FINISHED
    }

    /**
     * Makes a Http Get request to the grab path and returns the response
     * @param jobParameters that contain information like the path, host, port, etc.
     * @return the httpResponse
     */
    private static HttpResponse doRequest(Map jobParameters) {
        final String path = jobParameters.get(ClientBatchJob.PATH)
        final String excludePathParam = jobParameters.get(ClientBatchJob.EXCLUDE_PATHS)
        final excludePaths = (excludePathParam != null && !excludePathParam.isEmpty() ? excludePathParam.split(/\*/) : Collections.EMPTY_LIST) as Collection<String>
        final String host = jobParameters.get(ClientBatchJob.HOST)
        final String port = jobParameters.get(ClientBatchJob.PORT)
        final String username = jobParameters.get(ClientBatchJob.SERVER_USERNAME)
        final String password = jobParameters.get(ClientBatchJob.SERVER_PASSWORD)
        final String contentAfterDate = jobParameters.get(ClientBatchJob.CONTENT_AFTER_DATE) ?: ""

        final String encodedContentAfterDate = URLEncoder.encode(contentAfterDate, 'utf-8')
        final String encodedPath = URLEncoder.encode(path, 'utf-8')

        URIBuilder uriBuilder = new URIBuilder(scheme: "http", host: host, port: port as Integer, path: "/grabbit/job")
        uriBuilder.addParameter("path", encodedPath)
        uriBuilder.addParameter("after", encodedContentAfterDate)
        for(String excludePath : excludePaths) {
            uriBuilder.addParameter("excludePath", URLEncoder.encode(excludePath, 'UTF-8'))
        }

        //create the get request
        HttpGet get = new HttpGet(uriBuilder.build())
        HttpClient client = getHttpClient(username, password)
        HttpClientContext context = getHttpClientContext(get)
        HttpResponse response = client.execute(get, context)
        response
    }

    /**
     * Gets a Http Get connection for the provided authentication information
     * @param username
     * @param password
     * @return a {@link org.apache.http.impl.client.DefaultHttpClient} instance
     */
    private static DefaultHttpClient getHttpClient(final String username, final String password) {
        DefaultHttpClient client = new DefaultHttpClient()

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider()
        credentialsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password)
        )
        client.setCredentialsProvider(credentialsProvider)
        client
    }

    /**
     * Gets a new HttpClientContext for the given HttpRequest. The ClientContext sets up preemptive
     * authentication using BasicAuthCache
     * @param get the HttpGet request
     * @return the HttpClientContext for given HttpRequest
     */
    private static HttpClientContext getHttpClientContext(HttpGet get) {
        // Setup preemptive Authentication
        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache()
        HttpHost host = new HttpHost(get.URI.host, get.URI.port, get.URI.scheme)
        authCache.put(host, new BasicScheme())
        // Add AuthCache to the execution context
        HttpClientContext context = HttpClientContext.create()
        context.setAuthCache(authCache)
        return context
    }

}
