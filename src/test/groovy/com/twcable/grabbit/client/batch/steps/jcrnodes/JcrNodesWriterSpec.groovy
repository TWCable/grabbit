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
import com.twcable.grabbit.jcr.JCRUtil
import com.twcable.grabbit.proto.NodeProtos
import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import com.twcable.grabbit.proto.NodeProtos.Node.Builder as ProtoNodeBuilder
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import com.twcable.grabbit.proto.NodeProtos.Property.Builder as ProtoPropertyBuilder
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import org.apache.jackrabbit.JcrConstants
import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Node as JcrNode
import javax.jcr.PathNotFoundException
import javax.jcr.Session

import static com.day.cq.commons.jcr.JcrConstants.*
import static com.twcable.jackalope.JCRBuilder.repository
import static javax.jcr.PropertyType.LONG
import static javax.jcr.PropertyType.STRING
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED

@Subject(JcrNodesWriter)
class JcrNodesWriterSpec extends Specification {

    def cleanup() {
        ClientBatchJobContext.cleanup()
    }

    def "Can get a Jcr File Node given a single Protobuf Message Node"() {
        given:

        ProtoNodeBuilder nodeBuilder = ProtoNode.newBuilder()
        nodeBuilder.setName("/default.groovy")

        ProtoProperty propertyOne = ProtoProperty
            .newBuilder()
            .setName(JCR_PRIMARYTYPE)
            .setType(STRING)
            .setMultiple(false)
            .addValues(ProtoValue.newBuilder().setStringValue(NT_FILE))
            .build()

        ProtoProperty propertyTwo = ProtoProperty
            .newBuilder()
            .setName(JCR_LASTMODIFIED)
            .setType(STRING)
            .setMultiple(false)
            .addValues(NodeProtos.Value.newBuilder().setStringValue("Date"))
            .build()

        nodeBuilder.addAllProperties([propertyOne, propertyTwo])

        // Child Node jcr:content
        ProtoNodeBuilder childNodeBuilder = ProtoNode.newBuilder()
        childNodeBuilder.setName("/default.groovy/$JcrConstants.JCR_CONTENT")

        ProtoProperty childProperty = NodeProtos.Property
                .newBuilder()
                .setName(JCR_PRIMARYTYPE)
                .setType(STRING)
                .setMultiple(false)
                .addValues(NodeProtos.Value.newBuilder().setStringValue(NT_RESOURCE))
                .build()

        childNodeBuilder.addProperties(childProperty)

        // Adding Child Node
        nodeBuilder.addMandatoryChildNode(childNodeBuilder.build())

        NodeProtos.Node nodeProto = nodeBuilder.build()

        Session session = JCRUtil.getSession(repository().build(), "admin")

        when:
        ClientBatchJobContext.setSession(session)
        new JcrNodesWriter().write([nodeProto])

        then:
        JcrNode jcrNode = session.getNode("/default.groovy")
        jcrNode != null
        jcrNode.hasProperties()
        JcrNode childNode = session.getNode("/default.groovy/$JcrConstants.JCR_CONTENT")
        childNode != null

    }


    def "Can get a Jcr Unstructured Node given a single Protobuf Message Node"() {
        ProtoNodeBuilder nodeBuilder = ProtoNode.newBuilder()
        nodeBuilder.setName("/default")

        ProtoProperty propertyOne = NodeProtos.Property
            .newBuilder()
            .setName("jcr:primaryType")
            .setType(STRING)
            .setMultiple(false)
            .addValues(NodeProtos.Value.newBuilder().setStringValue("nt:unstructured"))
            .build()

        ProtoProperty propertyTwo = NodeProtos.Property
            .newBuilder()
            .setName("multiValuedLong")
            .setType(LONG)
            .setMultiple(true)
            .addAllValues(
                [
                    ProtoValue.newBuilder().setStringValue("12345").build(),
                    ProtoValue.newBuilder().setStringValue("54321").build()
                ]
            )
            .build()

        nodeBuilder.addAllProperties([propertyOne, propertyTwo])

        ProtoNode protoNode = nodeBuilder.build()

        Session session = JCRUtil.getSession(repository().build(), "admin")

        when:
        ClientBatchJobContext.setSession(session)
        new JcrNodesWriter().write([protoNode])

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

        Session session = JCRUtil.getSession(repository().build(), "admin")

        when:
        ClientBatchJobContext.setSession(session)
        new JcrNodesWriter().write(mockNodeProtos)

        then:
        session.getNode("/foo") != null
        session.getNode("/foo/bar") != null
        session.getNode("/foo/bar/foo") != null
        notThrown(PathNotFoundException)
    }

    def getNodeProto(String name, String primaryTypeName, String primaryTypeValue) {
        ProtoNodeBuilder nodeBuilder = ProtoNode.newBuilder()
        nodeBuilder.setName(name)

        ProtoPropertyBuilder propertyBuilder = ProtoProperty.newBuilder()
                .setName(primaryTypeName)
                .setType(STRING)
                .setMultiple(false)
                .addValues(ProtoValue.newBuilder().setStringValue(primaryTypeValue))

        nodeBuilder.addProperties(propertyBuilder.build())
        nodeBuilder.build()
    }

}
