package com.twc.webcms.sync.utils.unmarshaller

import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.proto.NodeProtos
import spock.lang.Specification

import javax.jcr.Node as JcrNode
import javax.jcr.Session

import static com.twc.jackalope.JCRBuilder.repository

class ProtobufUnmarshallerSpec extends Specification {

    def "Can get a Jcr Node given a single Protobuf Message Node"() {
        given:
        NodeProtos.Node.Builder nodeBuilder = NodeProtos.Node.newBuilder()
        nodeBuilder.setName("/default.groovy")
        NodeProtos.Properties.Builder propertiesBuilder = NodeProtos.Properties.newBuilder()
        NodeProtos.Property aProperty = NodeProtos.Property
                .newBuilder()
                .setName("jcr:primaryType")
                .setType(1)
                .setValue("nt:file")
                .build()

        propertiesBuilder.addProperty(aProperty)
        aProperty = NodeProtos.Property
                .newBuilder()
                .setName("jcr:lastModified")
                .setType(1)
                .setValue("Date")
                .build()
        propertiesBuilder.addProperty(aProperty)
        nodeBuilder.setProperties(propertiesBuilder.build())
        NodeProtos.Node nodeProto = nodeBuilder.build()

        Session session = JcrUtil.getSession(repository().build(), "admin")

        when:
        new ProtobufUnmarshaller().unmarshall(nodeProto, session)

        then:
        JcrNode jcrNode = session.getNode("/default.groovy")
        jcrNode != null
        jcrNode.hasProperties()
        JcrNode jcrAnotherNode = session.getNode("/default.groovy/${JcrNode.JCR_CONTENT}")
        jcrAnotherNode != null
    }

}