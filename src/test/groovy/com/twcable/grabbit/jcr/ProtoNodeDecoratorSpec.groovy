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
package com.twcable.grabbit.jcr

import com.twcable.grabbit.proto.NodeProtos
import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import com.twcable.grabbit.proto.NodeProtos.Node.Builder as ProtoNodeBuilder
import spock.lang.Specification


import static javax.jcr.PropertyType.STRING
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@SuppressWarnings("GroovyAccessibility")
class ProtoNodeDecoratorSpec extends Specification {

    def "ProtoNodeDecorator can not be constructed with a null ProtoNode"() {
        when:
        ProtoNodeDecorator.createFrom(null)

        then:
        thrown(IllegalArgumentException)
    }


    def "Can create a regular DefaultProtoNodeDecorator"() {
        given:
        ProtoNodeBuilder nodeBuilder = ProtoNode.newBuilder()
        nodeBuilder.setName("user")
        NodeProtos.Property primaryTypeProperty = NodeProtos.Property
                .newBuilder()
                .setName(JCR_PRIMARYTYPE)
                .setType(STRING)
                .setMultiple(false)
                .addValues(NodeProtos.Value.newBuilder().setStringValue('nt:unstructured'))
                .build()
        nodeBuilder.addProperties(primaryTypeProperty)
        final protoNodeDecorator = ProtoNodeDecorator.createFrom(nodeBuilder.build())

        expect:
        protoNodeDecorator instanceof DefaultProtoNodeDecorator
    }


    def "Can create an AuthorizableProtoNodeDecorator with wrapped User node"() {
        given:
        NodeProtos.Node.Builder nodeBuilder = NodeProtos.Node.newBuilder()
        nodeBuilder.setName("user")
        NodeProtos.Property userProperty = NodeProtos.Property
                .newBuilder()
                .setName(JCR_PRIMARYTYPE)
                .setType(STRING)
                .setMultiple(false)
                .addValues(NodeProtos.Value.newBuilder().setStringValue('rep:User'))
                .build()
        nodeBuilder.addProperties(userProperty)
        final protoNodeDecorator = DefaultProtoNodeDecorator.createFrom(nodeBuilder.build())

        expect:
        protoNodeDecorator instanceof AuthorizableProtoNodeDecorator
    }


    def "Can create an AuthorizableProtoNodeDecorator with wrapped Group node"() {
        given:
        NodeProtos.Node.Builder nodeBuilder = NodeProtos.Node.newBuilder()
        nodeBuilder.setName("group")
        NodeProtos.Property groupProperty = NodeProtos.Property
                .newBuilder()
                .setName(JCR_PRIMARYTYPE)
                .setType(STRING)
                .setMultiple(false)
                .addValues(NodeProtos.Value.newBuilder().setStringValue('rep:Group'))
                .build()
        nodeBuilder.addProperties(groupProperty)
        final protoNodeDecorator = DefaultProtoNodeDecorator.createFrom(nodeBuilder.build())

        expect:
        protoNodeDecorator instanceof AuthorizableProtoNodeDecorator
    }
}
