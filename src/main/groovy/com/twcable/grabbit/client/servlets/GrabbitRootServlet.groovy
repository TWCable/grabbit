package com.twcable.grabbit.client.servlets

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingAllMethodsServlet
import static javax.servlet.http.HttpServletResponse.SC_OK

/**
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
 *
 * Root resource for /grabbit requests.
 * Acts as a handler for {@link com.twcable.grabbit.resources.RootResource} resource.
 */
@Slf4j
@CompileStatic
@SlingServlet(methods = ['GET'], resourceTypes = ['twcable:grabbit'])
class GrabbitRootServlet extends  SlingAllMethodsServlet {

    /**
     * This method gets called when a request is made to either Grabbit Root resource directly (/grabbit) or any invalid
     * URL under it.
     * Response will contain links to valid resources' urls under Grabbit Root url, in accordance with HATEOAS.
     * A hypermedia-driven response provides information to navigate Grabbit's resources dynamically, by including
     * valid hypermedia links along with the response.
     * For reference, {@see https://spring.io/understanding/HATEOAS}
     *
     * From Grabbit Root resource, Valid resources are
     * - Grabbit's Job Resource (/grabbit/job)
     * - Grabbit's Content Resource (/grabbit/content)
     * - Grabbit's Transaction Resource (/grabbit/transaction)
     */
    @Override
    void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        response.setContentType("text/html")
        response.setStatus(SC_OK)
        response.writer.write this.class.getResourceAsStream("RootResource.txt").text
    }
}
