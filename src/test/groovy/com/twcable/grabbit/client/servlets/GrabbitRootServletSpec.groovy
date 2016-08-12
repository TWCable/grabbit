package twcable.grabbit.client.servlets

import com.twcable.grabbit.client.servlets.GrabbitRootServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.resource.Resource
import static javax.servlet.http.HttpServletResponse.SC_OK
import spock.lang.Specification
import spock.lang.Unroll

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
class GrabbitRootServletSpec extends Specification {

    @Unroll
    def "Request to #path will result in OK response"() {
        given:
        final request = Mock(SlingHttpServletRequest) {
            getResource() >> Mock(Resource) {
            }
            getPathInfo() >> "/grabbit/${path}"
        }
        final response = Mock(SlingHttpServletResponse) {
            1 * setStatus(SC_OK)
            getWriter() >> Mock(PrintWriter)
        }

        final grabbitRootServlet = new GrabbitRootServlet()

        expect:
        grabbitRootServlet.doGet(request, response)

        where:
        path << ["", null, "invalid", 1]
    }
}
