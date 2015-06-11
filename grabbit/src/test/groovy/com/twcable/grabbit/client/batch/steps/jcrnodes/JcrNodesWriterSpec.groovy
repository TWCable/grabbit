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

package com.twcable.grabbit.client.batch.steps.jcrnodes

import com.twcable.grabbit.client.batch.ClientBatchJobContext
import com.twcable.grabbit.jcr.JcrUtil
import com.twcable.grabbit.proto.NodeProtos
import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Node as JcrNode
import javax.jcr.PathNotFoundException
import javax.jcr.Session

import static com.twcable.jackalope.JCRBuilder.repository
import static javax.jcr.PropertyType.LONG
import static javax.jcr.PropertyType.STRING
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED

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
        jcrNode.getProperty(JCR_LASTMODIFIED) != null
        jcrNode.hasProperties()
        jcrNode.getProperty("multiValuedLong").values.length == 2
    }

    def "Can get a Jcr Node where a Node Name appears multiple times in the path"() {
        given:
        List mockNodeProtos = [
            getNodeProto("/foo", "jcr:primaryType", "nt:unstructured"),
            getNodeProto("/foo/bar", "jcr:primaryType", "nt:unstructured"),
            getNodeProto("/foo/bar/foo", "jcr:primaryType", "nt:file")
        ]

        Session session = JcrUtil.getSession(repository().build(), "admin")

        when:
        ClientBatchJobContext.THREAD_LOCAL.set(new ClientBatchJobContext(null, session))
        new JcrNodesWriter().write(mockNodeProtos)

        then:
        session.getNode("/foo") != null
        session.getNode("/foo/bar") != null
        session.getNode("/foo/bar/foo") != null
        notThrown(PathNotFoundException)
    }

    def getNodeProto(String name, String primaryTypeName, String primaryTypeValue) {
        NodeProtos.Node.Builder nodeBuilder = NodeProtos.Node.newBuilder()
        NodeProtos.Property.Builder propertyBuilder =
                NodeProtos.Property.newBuilder()
                .setName(primaryTypeName)
                .setType(STRING)
                .setValue(NodeProtos.Value.newBuilder().setStringValue(primaryTypeValue))
        nodeBuilder.setName(name)
        nodeBuilder.properties =
                NodeProtos.Properties.newBuilder()
                .addProperty(propertyBuilder.build())
                .build()
        nodeBuilder.build()
    }

}
