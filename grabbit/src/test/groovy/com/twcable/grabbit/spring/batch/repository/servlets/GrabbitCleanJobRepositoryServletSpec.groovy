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

package com.twcable.grabbit.spring.batch.repository.servlets

import com.twcable.grabbit.spring.batch.repository.services.CleanJobRepository
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import spock.lang.Specification
import spock.lang.Subject

@Subject(GrabbitCleanJobRepositoryServlet)
class GrabbitCleanJobRepositoryServletSpec extends Specification {

    def "Servlet handles the case when hours parameter is not passed"() {
        given:
        def servlet = new GrabbitCleanJobRepositoryServlet(cleanJobRepository: Mock(CleanJobRepository))
        def request = Mock(SlingHttpServletRequest)
        request.getParameter("hours") >> null
        def response = Mock(SlingHttpServletResponse)
        def writer = new StringWriter()
        response.getWriter() >> new PrintWriter(writer)
        when:
        servlet.doPost(request, response)

        then:
        writer != null
        writer.toString() == "Parameter 'hours' must be an integer"
    }

    def "Servlet handles the case when hours parameter is correctly passed"() {
        given:
        def clientJobRepository = Mock(CleanJobRepository)
        clientJobRepository.cleanJobRepository(_) >> (["id1","id2","id3"] as List<String>)
        def servlet = new GrabbitCleanJobRepositoryServlet(cleanJobRepository: clientJobRepository)
        def request = Mock(SlingHttpServletRequest)
        request.getParameter("hours") >> 5
        def response = Mock(SlingHttpServletResponse)
        def writer = new StringWriter()
        response.getWriter() >> new PrintWriter(writer)
        when:
        servlet.doPost(request, response)

        then:
        writer != null
        writer.toString().contains("JobExecutionsIds that were removed")
        writer.toString().contains("[id1, id2, id3]")
    }
}
