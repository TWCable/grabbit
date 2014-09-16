package com.twc.webcms.sync.utils.marshaller

import com.twc.jackalope.NodeBuilder as FakeNodeBuilder
import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.proto.NodeProtos.Node
import spock.lang.Specification

import javax.jcr.Node as JcrNode

import static com.twc.jackalope.JCRBuilder.node
import static com.twc.jackalope.JCRBuilder.property
import static com.twc.jackalope.JcrConstants.NT_FILE

class ProtobufMarshallerSpec extends Specification {

    def "Can get a Protobuf Message Node given a single Jcr Node"() {
        given:
        FakeNodeBuilder fakeNodeBuilder =
                            node("default.groovy",
                                node("jcr:content",
                                    property("jcr:data", "foo" )
                                ),
                                property("jcr:primaryType", NT_FILE),
                                property("jcr:lastModified", "Date")
                            )
        JcrNode aJcrNode = fakeNodeBuilder.build()

        when:
        Node nodeProto = ProtobufMarshaller.marshall(aJcrNode)

        then:
        nodeProto != null
        nodeProto.hasProperties()
        nodeProto.properties.propertyList.size() == 2
        nodeProto.properties.propertyList.find {
            NodeProtos.Property property -> property.name == "jcr:primaryType"
        }.value == "nt:file"
        nodeProto.properties.propertyList.find {
            NodeProtos.Property property -> property.name == "jcr:lastModified"
        }.value == "Date"
    }

}
