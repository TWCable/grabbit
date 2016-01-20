package com.twcable.grabbit.server

import com.twcable.grabbit.server.services.ServerService
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import spock.lang.Specification
import spock.lang.Subject

import javax.servlet.ServletOutputStream

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_OK

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

@Subject(GrabbitContentPullServlet)
class GrabbitContentPullServletSpec extends Specification {


    def "Can pull a content stream from a path correctly"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getParameter("path") >> "%2Fsome%2Fpath"
            getParameterValues("excludePath") >> ["%2Fexclude%2Fme", "%2Fexclude%2Fme%2Ftoo"]
            getParameter("after") >> "2016-01-29T13%3A53%3A58.831-05%3A00"
            getRemoteUser() >> "user"
        }
        final outputStream = Mock(ServletOutputStream)
        final response = Mock(SlingHttpServletResponse) {
            1 * setContentType("application/octet-stream")
            1 * setStatus(SC_OK)
            getOutputStream() >> outputStream
        }
        final serverService = Mock(ServerService) {
            1 * getContentForRootPath("user", "/some/path", ["/exclude/me", "/exclude/me/too"], "2016-01-29T13:53:58.831-05:00", outputStream)
        }

        when:
        final contentPullServlet = new GrabbitContentPullServlet()
        contentPullServlet.setServerService(serverService)

        then:
        contentPullServlet.doGet(request, response)
    }

    def "Request for a content stream with no path results in a bad request response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getParameter("path") >> null
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_BAD_REQUEST)
            getWriter() >> Mock(PrintWriter) {
                1 * write(_)
            }
        }

        expect:
        new GrabbitContentPullServlet().doGet(request, response)
    }

}
