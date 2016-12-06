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
package com.twcable.grabbit.server

import com.twcable.grabbit.server.services.ServerService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingSafeMethodsServlet

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * This servlet is used to pull a stream of Grabbit content.
 *
 * A client request is made via {@link com.twcable.grabbit.client.batch.steps.http.CreateHttpConnectionTasklet} to
 * start the stream of content.
 */
@Slf4j
@CompileStatic
@SlingServlet(methods = ['GET'], resourceTypes = ['twcable:grabbit/content'])
class GrabbitContentPullServlet extends SlingSafeMethodsServlet {

    @Reference(bind = 'setServerService')
    ServerService serverService

    /**
     * This GET request starts a stream of Grabbit content. The servlet looks for several query parameters related
     * to a stream.
     *
     * <ul>
     *     <li><b>path</b> is the URL encoded path to the content on the server to be streamed. This is required.
     *     <li><b>excludePath</b> is a URL encoded sub-path to exclude from the stream. This can have multiple values. It is not required.
     *     <li><b>after</b> is a URL encoded ISO-8601 date that is used to stream delta content. It is not required.
     * </ul>
     *
     * {@link GrabbitContentPullServlet} will use the request remote user credentials to authenticate against the server JCR.
     *
     * @param request The request to process.
     * @param response Our response to the request.
     */
    @Override
    void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        final path = request.getParameter("path")
        if(!path) {
            response.status = SC_BAD_REQUEST
            response.writer.write("No path provided for content!")
            return
        }
        final excludePaths = request.getParameterValues("excludePath")

        final decodedPath = URLDecoder.decode(path, "utf-8")
        final decodedExcludePaths = excludePaths.collect { String ep -> URLDecoder.decode(ep, 'UTF-8') }
        response.contentType = "application/octet-stream"
        response.status = SC_OK

        final afterDateString = URLDecoder.decode(request.getParameter("after") ?: "", "utf-8")

        if(afterDateString) {
            log.info "Path : $decodedPath, Exclude Paths: $decodedExcludePaths, " +
                    "AfterDate String : $afterDateString. Will send only delta content."
        }

        //The Login of the user making this request.
        //This user will be used to connect to JCR.
        //If the User is null, 'anonymous' will be used to connect to JCR.
        final serverUsername = request.remoteUser
        serverService.getContentForRootPath(serverUsername,
                                            decodedPath,
                                            decodedExcludePaths ?: null,
                                            afterDateString ?: null,
                                            response.outputStream)
    }
}
