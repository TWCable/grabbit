package com.twc.webcms.sync.client.batch.steps.jcrnodes

import com.twc.webcms.sync.client.batch.ClientBatchJobContext
import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.proto.NodeProtos
import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Node as JcrNode
import javax.jcr.Session

import static com.twc.jackalope.JCRBuilder.repository
import static javax.jcr.PropertyType.LONG
import static javax.jcr.PropertyType.STRING

@Subject(JcrNodesWriter)
class JcrNodesWriterSpec extends Specification {

    def "Can get a Jcr File Node given a single Protobuf Message Node"() {
        given:
        NodeProtos.Node.Builder nodeBuilder = NodeProtos.Node.newBuilder()
        nodeBuilder.setName("/default.groovy")
        NodeProtos.Properties.Builder propertiesBuilder = NodeProtos.Properties.newBuilder()
        NodeProtos.Property aProperty = NodeProtos.Property
                .newBuilder()
                .setName("jcr:primaryType")
                .setType(STRING)
                .setValue(NodeProtos.Value.newBuilder().setStringValue("nt:file"))
                .build()

        propertiesBuilder.addProperty(aProperty)
        aProperty = NodeProtos.Property
                .newBuilder()
                .setName("jcr:lastModified")
                .setType(STRING)
                .setValue(NodeProtos.Value.newBuilder().setStringValue("Date"))
                .build()
        propertiesBuilder.addProperty(aProperty)
        nodeBuilder.setProperties(propertiesBuilder.build())
        NodeProtos.Node nodeProto = nodeBuilder.build()

        Session session = JcrUtil.getSession(repository().build(), "admin")

        when:
        ClientBatchJobContext.THREAD_LOCAL.set(new ClientBatchJobContext(null, session))
        new JcrNodesWriter().write([nodeProto])

        then:
        JcrNode jcrNode = session.getNode("/default.groovy")
        jcrNode != null
        jcrNode.hasProperties()
        JcrNode jcrAnotherNode = session.getNode("/default.groovy/${JcrNode.JCR_CONTENT}")
        jcrAnotherNode != null

    }

    def "Can get a Jcr Unstructured Node given a single Protobuf Message Node"() {
        NodeProtos.Node.Builder nodeBuilder = NodeProtos.Node.newBuilder()
        nodeBuilder.setName("/default")
        NodeProtos.Properties.Builder propertiesBuilder = NodeProtos.Properties.newBuilder()
        NodeProtos.Property aProperty = NodeProtos.Property
                .newBuilder()
                .setName("jcr:primaryType")
                .setType(STRING)
                .setValue(NodeProtos.Value.newBuilder().setStringValue("nt:unstructured"))
                .build()

        propertiesBuilder.addProperty(aProperty)

        aProperty = NodeProtos.Property
                .newBuilder()
                .setName("multiValuedLong")
                .setType(LONG)
                .setValues(
                NodeProtos.Values.newBuilder().addAllValue(
                        [
                                NodeProtos.Value.newBuilder().setStringValue("12345").build(),
                                NodeProtos.Value.newBuilder().setStringValue("54321").build()
                        ]
                )
        )
                .build()
        propertiesBuilder.addProperty(aProperty)
        nodeBuilder.setProperties(propertiesBuilder.build())
        NodeProtos.Node nodeProto = nodeBuilder.build()

        Session session = JcrUtil.getSession(repository().build(), "admin")

        when:
        ClientBatchJobContext.THREAD_LOCAL.set(new ClientBatchJobContext(null, session))
        new JcrNodesWriter().write([nodeProto])

        then:
        JcrNode jcrNode = session.getNode("/default")
        jcrNode != null
        jcrNode.hasProperties()
        jcrNode.getProperty("multiValuedLong").values.length == 2
    }

}
