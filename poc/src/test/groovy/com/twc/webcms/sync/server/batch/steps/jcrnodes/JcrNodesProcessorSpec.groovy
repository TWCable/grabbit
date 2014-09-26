package com.twc.webcms.sync.server.batch.steps.jcrnodes

import com.twc.jackalope.NodeBuilder as FakeNodeBuilder
import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.proto.NodeProtos.Node
import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Node as JcrNode

import static com.twc.jackalope.JCRBuilder.node
import static com.twc.jackalope.JCRBuilder.property
import static com.twc.jackalope.JcrConstants.NT_FILE
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
        Node nodeProto = jcrNodesProcessor.process(aJcrNode)
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
