package com.twc.webcms.sync.server.marshaller

import com.twc.jackalope.NodeBuilder as FakeNodeBuilder
import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.proto.NodeProtos.Node
import com.twc.webcms.sync.server.marshaller.ProtobufMarshaller
import spock.lang.Specification

import javax.jcr.Node as JcrNode

import static com.twc.jackalope.JCRBuilder.node
import static com.twc.jackalope.JCRBuilder.property
import static com.twc.jackalope.JcrConstants.NT_FILE
import static javax.jcr.PropertyType.*

class ProtobufMarshallerSpec extends Specification {

    def "Can get a Protobuf Message Node given a single Jcr Node"() {
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
        Node nodeProto = ProtobufMarshaller.toNodeProto(aJcrNode)
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
