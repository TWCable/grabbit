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
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import okhttp3.*
import okhttp3.OkHttpClient.Builder as HttpClientBuilder
import okhttp3.Request.Builder as RequestBuilder
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

import javax.annotation.Nonnull
import java.util.concurrent.TimeUnit

import static javax.servlet.http.HttpServletResponse.SC_OK
import static okhttp3.HttpUrl.Builder as HttpUrlBuilder

/**
 * This tasklet opens an HTTP connection for the current job, and places the input stream on the {@link ClientBatchJobContext}
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

        final Connection connection = createConnection(jobParameters)

        ClientBatchJobContext.setInputStream(connection.inputStream)

        if(connection.status != SC_OK) {
            log.warn "Received a status of ${connection.status} when attempting to create a connection to ${jobParameters.get(ClientBatchJob.HOST)}. Bailing out. See debug log for more details."
            log.debug "Request to start a connection with ${jobParameters.get(ClientBatchJob.HOST)} resulted in: ${connection.networkResponse}."
            contribution.setExitStatus(ExitStatus.FAILED)
            return RepeatStatus.FINISHED
        }

        return RepeatStatus.FINISHED
    }


    Connection createConnection(@Nonnull final Map jobParameters) {

        final String username = (String)jobParameters.get(ClientBatchJob.SERVER_USERNAME)
        final String password = (String)jobParameters.get(ClientBatchJob.SERVER_PASSWORD)

        final Request request = new RequestBuilder()
                .url(getURLForRequest(jobParameters))
                .addHeader('Authorization', Credentials.basic(username, password))
                .build()


        final OkHttpClient client = getNewHttpClient()

        final Response response = client.newCall(request).execute()
        //We return response information in a connection like this because it's clear, but also because Response is a final class that we can not easily mock
        return new Connection(response.body().byteStream(), response.networkResponse(), response.code())
    }


    HttpUrl getURLForRequest(@Nonnull final Map jobParameters) {
        HttpUrlBuilder urlBuilder = new HttpUrl.Builder()

        urlBuilder.scheme((String)jobParameters.get(ClientBatchJob.SCHEME))
        urlBuilder.host((String)jobParameters.get(ClientBatchJob.HOST))
        urlBuilder.port(Integer.parseInt((String)jobParameters.get(ClientBatchJob.PORT)))
        urlBuilder.encodedPath('/grabbit/content')

        //addQueryParameter will encode these values for us
        urlBuilder.addQueryParameter('path', (String)jobParameters.get(ClientBatchJob.PATH))
        urlBuilder.addQueryParameter('after', (String)jobParameters.get(ClientBatchJob.CONTENT_AFTER_DATE) ?: '')

        final String excludePathParam = jobParameters.get(ClientBatchJob.EXCLUDE_PATHS)
        final excludePaths = (excludePathParam != null && !excludePathParam.isEmpty() ? excludePathParam.split(/\*/) : Collections.EMPTY_LIST) as Collection<String>
        for(String excludePath : excludePaths) {
            urlBuilder.addQueryParameter('excludePath', excludePath)
        }

        return urlBuilder.build()
    }


    private OkHttpClient getNewHttpClient() {
        return new HttpClientBuilder()
                .connectTimeout(1L, TimeUnit.MINUTES)
                .readTimeout(1L, TimeUnit.MINUTES)
                .writeTimeout(1L, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
                .build()
    }


    @CompileStatic
    @Canonical
    static class Connection {
        InputStream inputStream
        Response networkResponse
        int status
    }
}
