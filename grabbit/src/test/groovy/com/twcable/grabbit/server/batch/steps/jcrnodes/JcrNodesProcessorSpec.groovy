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

package com.twcable.grabbit.server.batch.steps.jcrnodes

import com.twcable.jackalope.NodeBuilder as FakeNodeBuilder
import com.twcable.grabbit.proto.NodeProtos
import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Node as JcrNode

import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property
import static com.twcable.jackalope.JcrConstants.NT_FILE
import static javax.jcr.PropertyType.LONG
import static javax.jcr.PropertyType.STRING

@Subject(JcrNodesProcessor)
class JcrNodesProcessorSpec extends Specification {

    def "Can marshall a JCR Node to a Protobuf Message"() {
        given:
        FakeNodeBuilder fakeNodeBuilder =
                node("default.groovy",
                        node("jcr:content",
                                property("jcr:data", "foo" )
                        ),
                        property("jcr:primaryType", NT_FILE),
                        property("jcr:lastModified", "Date"),
                        property("multiValueLong", [1L,2L,4L] as Object[]),
                        property("multiValueString", ["a", "b", "c"] as Object[]),

                )
        JcrNode aJcrNode = fakeNodeBuilder.build()

        when:
        JcrNodesProcessor jcrNodesProcessor = new JcrNodesProcessor()
        NodeProtos.Node nodeProto = jcrNodesProcessor.process(aJcrNode)
        NodeProtos.Property propertyLong = nodeProto.properties.propertyList.find { it.name == "multiValueLong" }
        NodeProtos.Property propertyString = nodeProto.properties.propertyList.find { it.name == "multiValueString" }

        then:
        nodeProto != null
        nodeProto.hasProperties()

        propertyLong.hasValues()
        !propertyLong.hasValue()
        propertyLong.values.valueCount == 3
        propertyLong.type == LONG


        propertyString.hasValues()
        !propertyString.hasValue()
        propertyString.values.valueCount == 3
        propertyString.type == STRING
    }

}
