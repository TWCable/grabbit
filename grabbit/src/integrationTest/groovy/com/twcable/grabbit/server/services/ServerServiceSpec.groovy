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

package com.twcable.grabbit.server.services

import com.twcable.grabbit.server.services.impl.DefaultServerService
import com.twcable.grabbit.testutils.MockServletOutputStream
import com.twcable.jackalope.NodeBuilder as FakeNodeBuilder
import org.apache.sling.jcr.api.SlingRepository
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property
import static com.twcable.jackalope.JCRBuilder.repository
import static com.twcable.jackalope.JcrConstants.NT_FILE

@Subject(ServerService)
class ServerServiceSpec extends Specification {

    @Shared
    SlingRepository slingRepository

    @Shared
    ConfigurableApplicationContext configurableApplicationContext

    @Shared
    ServerService syncServerService


    def setupSpec() {
        configurableApplicationContext = new ClassPathXmlApplicationContext("META-INF/spring/server-batch-job.xml")

        FakeNodeBuilder fakeNodeBuilder =
            node("default.groovy",
                node("jcr:content",
                    property("jcr:data", "foo")
                ),
                property("jcr:primaryType", NT_FILE),
                property("jcr:lastModified", "Date"),
                property("multiValueLong", [1L, 2L, 4L] as Object[]),
                property("multiValueString", ["a", "b", "c"] as Object[]),
            )


        slingRepository = repository(fakeNodeBuilder).build()

        syncServerService = new DefaultServerService(slingRepository: slingRepository,
            configurableApplicationContext: configurableApplicationContext)
    }


    def "Service should write data to provided outputStream"() {
        given:
        MockServletOutputStream mockServletOutputStream = new MockServletOutputStream()

        when:
        //This will also actually execute the Batch Job internally
        syncServerService.getContentForRootPath("/default.groovy", (Collection<String>)Collections.EMPTY_LIST, "", mockServletOutputStream)

        then:
        mockServletOutputStream != null
        mockServletOutputStream.toString().contains("default.groovy")
    }

    def "Service excludes data in excludePaths and writes rest of the data to provided outputStream"() {
        given:
        MockServletOutputStream mockServletOutputStream = new MockServletOutputStream()

        when:
        //This will also actually execute the Batch Job internally
        syncServerService.getContentForRootPath("/default.groovy", ["/default.groovy/jcr:content"] as Collection<String>, "", mockServletOutputStream)

        then:
        mockServletOutputStream != null
        !mockServletOutputStream.toString().contains("jcr:content")
    }

}
