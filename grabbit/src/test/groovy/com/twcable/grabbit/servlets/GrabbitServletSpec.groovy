package com.twcable.grabbit.servlets

import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import spock.lang.Specification

import javax.annotation.Nonnull
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletResponse

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

class GrabbitServletSpec extends Specification {

    def "A 400 (Bad Request) is returned if the configuration provided is malformed or incorrect"() {
        given:
        SlingHttpServletRequest request = Mock(SlingHttpServletRequest)
        SlingHttpServletResponse response = Mock(SlingHttpServletResponse)
        GrabbitServlet servlet = new GrabbitServlet()

        when:
        request.getInputStream() >> inputStream
        request.getCharacterEncoding() >> "UTF-8"
        request.getRemoteUser() >> "admin"

        response.getWriter() >> Mock(PrintWriter)

        servlet.doPut(request, response)

        then:
        1 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        1 * response.setContentType("application/json")

        where:
        inputStream << [new StubServletInputStream(" "), new StubServletInputStream("foo: 'foo'")]
        //One causes SnakeYAML to produce a null config map, and the other does not pass our validations (missing values)
    }

    class StubServletInputStream extends ServletInputStream {


        private final int byte_length
        private final byte[] bytes
        private int byte_index = 0

        StubServletInputStream(@Nonnull final String data) {
            bytes = data as byte[]
            byte_length = bytes.length
        }

        @Override
        int read() throws IOException {
            if (byte_index <= byte_length - 1) {
                final thisByte = bytes[byte_index] as int
                byte_index++
                return thisByte
            }
            return -1
        }
    }

}
