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

import com.day.cq.commons.jcr.JcrConstants
import com.twcable.grabbit.proto.NodeProtos
import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import com.twcable.grabbit.proto.NodeProtos.Node.Builder as ProtoNodeBuilder
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.PropertyIterator
import javax.jcr.Session
import javax.jcr.nodetype.NodeType
import spock.lang.Specification


import static javax.jcr.PropertyType.REFERENCE
import static javax.jcr.PropertyType.STRING
import static javax.jcr.PropertyType.WEAKREFERENCE
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

class DefaultProtoNodeDecoratorSpec extends Specification {

    ProtoNode decoratedProtoNode
    ProtoProperty mixinProperty
    ProtoProperty someOtherProperty
    ProtoProperty referenceProperty
    ProtoProperty referencePropertyTwo


    def setup() {
        ProtoProperty primaryTypeProperty = NodeProtos.Property
            .newBuilder()
            .setName(JCR_PRIMARYTYPE)
            .setType(STRING)
            .setMultiple(false)
            .addValues(NodeProtos.Value.newBuilder().setStringValue(JcrConstants.NT_UNSTRUCTURED))
            .build()

        mixinProperty = NodeProtos.Property
            .newBuilder()
            .setName(JCR_MIXINTYPES)
            .setType(STRING)
            .setMultiple(true)
            .addAllValues(
                [
                    NodeProtos.Value.newBuilder().setStringValue("somemixintype").build(),
                    NodeProtos.Value.newBuilder().setStringValue("unwritablemixin").build()
                ]
            ).build()

        someOtherProperty = NodeProtos.Property
            .newBuilder()
            .setName("someproperty")
            .setType(STRING)
            .setMultiple(false)
            .addValues(NodeProtos.Value.newBuilder().setStringValue("somevalue"))
            .build()

        referenceProperty = NodeProtos.Property
            .newBuilder()
            .setName('jcr:uuid')
            .setType(WEAKREFERENCE)
            .setMultiple(false)
            .addValues(NodeProtos.Value.newBuilder().setStringValue('21232f29-7a57-35a7-8389-4a0e4a801fc3'))
            .build()

        referencePropertyTwo = NodeProtos.Property
                .newBuilder()
                .setName('jcr:uuid')
                .setType(REFERENCE)
                .setMultiple(false)
                .addValues(NodeProtos.Value.newBuilder().setStringValue('41232f29-7a57-35a7-8389-4a0e4a801fc6'))
                .build()

        ProtoNodeBuilder nodeBuilder = ProtoNode.newBuilder()
        nodeBuilder.setName("somenode")
        nodeBuilder.addProperties(primaryTypeProperty)
        nodeBuilder.addProperties(mixinProperty)
        nodeBuilder.addProperties(someOtherProperty)
        nodeBuilder.addProperties(referenceProperty)
        nodeBuilder.addProperties(referencePropertyTwo)
        decoratedProtoNode = nodeBuilder.build()
    }


    def "Can get primary type"() {
        when:
        final protoNodeDecorator = DefaultProtoNodeDecorator.createFrom(decoratedProtoNode)

        then:
        protoNodeDecorator.getPrimaryType().getStringValue() == JcrConstants.NT_UNSTRUCTURED
    }


    def "can get mixin property"() {
        given:
        final protoNodeDecorator = DefaultProtoNodeDecorator.createFrom(decoratedProtoNode)

        when:
        final property = protoNodeDecorator.getMixinProperty()

        then:
        property.valuesCount == 2
        property.name == JCR_MIXINTYPES
    }


    def "Can get just writable properties"() {
        given:
        final protoNodeDecorator = DefaultProtoNodeDecorator.createFrom(decoratedProtoNode)

        when:
        final Collection<ProtoPropertyDecorator> properties = protoNodeDecorator.getWritableProperties()

        then:
        properties.size() == 3
    }

    def "Can write this ProtoNodeDecorator to the JCR"() {
        given:
        final session = Mock(Session)
        final jcrNodeRepresentation = Mock(Node) {
            canAddMixin('somemixintype') >> true
            canAddMixin('unwritablemixin') >> false
            1 * addMixin('somemixintype')
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
            it.getPrimaryNodeType() >> Mock(NodeType) {
                it.canSetProperty(_, _) >> false
            }
        }
        final protoPropertyDecorators = [
                new ProtoPropertyDecorator(mixinProperty),
                new ProtoPropertyDecorator(someOtherProperty),
                new ProtoPropertyDecorator(referenceProperty),
                new ProtoPropertyDecorator(referencePropertyTwo)
        ]
        final protoNodeDecorator = Spy(DefaultProtoNodeDecorator, constructorArgs: [decoratedProtoNode, protoPropertyDecorators, null]) {
            getOrCreateNode(session) >>  {
                return jcrNodeRepresentation
            }
            writeMandatoryPieces(session, 'somenode') >> [
                Mock(JCRNodeDecorator) {
                    isReferenceable() >> true
                    getTransferredID() >> '21232f29-7a57-35a7-8389-4a0e4a801fc3'
                    getIdentifier() >> '31232f29-7a57-35a7-8389-4a0e4a801fc4'
                }
            ]
        }

        final jcrNodeDecorator = protoNodeDecorator.writeToJcr(session)

        expect:
        jcrNodeDecorator != null
    }
}
