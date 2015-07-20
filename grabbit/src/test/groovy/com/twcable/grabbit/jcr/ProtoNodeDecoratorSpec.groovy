package com.twcable.grabbit.jcr

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

import com.day.cq.commons.jcr.JcrConstants
import com.twcable.grabbit.proto.NodeProtos
import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import spock.lang.Specification

import javax.jcr.Node
import javax.jcr.Session

import static javax.jcr.PropertyType.STRING
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@SuppressWarnings("GroovyAccessibility")
class ProtoNodeDecoratorSpec extends Specification {

    ProtoNode decoratedProtoNode


    def setup() {
        NodeProtos.Node.Builder nodeBuilder = NodeProtos.Node.newBuilder()
        nodeBuilder.setName("somenode")

        NodeProtos.Properties.Builder propertiesBuilder = NodeProtos.Properties.newBuilder()

        NodeProtos.Property primaryTypeProperty = NodeProtos.Property
                .newBuilder()
                .setName(JCR_PRIMARYTYPE)
                .setType(STRING)
                .setValue(NodeProtos.Value.newBuilder().setStringValue(JcrConstants.NT_UNSTRUCTURED))
                .build()
        propertiesBuilder.addProperty(primaryTypeProperty)

        NodeProtos.Property mixinTypeProperty = NodeProtos.Property
                .newBuilder()
                .setName(JCR_MIXINTYPES)
                .setType(STRING)
                .setValues(
                NodeProtos.Values.newBuilder().addAllValue(
                    [
                        NodeProtos.Value.newBuilder().setStringValue("somemixintype").build(),
                        NodeProtos.Value.newBuilder().setStringValue("unwritablemixin").build()
                    ]
                ))
                .build()
        propertiesBuilder.addProperty(mixinTypeProperty)


        NodeProtos.Property someOtherProperty = NodeProtos.Property
                .newBuilder()
                .setName("someproperty")
                .setType(STRING)
                .setValue(NodeProtos.Value.newBuilder().setStringValue("somevalue"))
                .build()
        propertiesBuilder.addProperty(someOtherProperty)

        nodeBuilder.setProperties(propertiesBuilder.build())
        decoratedProtoNode = nodeBuilder.build()
    }


    def "ProtoNodeDecorator can not be constructed with a null ProtoNode"() {
        when:
        new ProtoNodeDecorator(null)

        then:
        thrown(IllegalArgumentException)
    }


    def "Can get primary type"() {
        when:
        final protoNodeDecorator = new ProtoNodeDecorator(decoratedProtoNode)

        then:
        protoNodeDecorator.getPrimaryType() == JcrConstants.NT_UNSTRUCTURED
    }


    def "can get mixin property"() {
        given:
        final protoNodeDecorator = new ProtoNodeDecorator(decoratedProtoNode)

        when:
        final property = protoNodeDecorator.getMixinProperty()

        then:
        property.values.valueCount == 2
        property.name == JCR_MIXINTYPES
    }


    def "Can get just writable properties"() {
        given:
        final protoNodeDecorator = new ProtoNodeDecorator(decoratedProtoNode)

        when:
        final properties = protoNodeDecorator.getWritableProperties()

        then:
        properties.size() == 1
        properties[0].value.stringValue == "somevalue"
    }


    def "Can write the decorated node to the JCR"() {
        given:
        final session = Mock(Session)
        final node = Mock(Node) {
            canAddMixin("somemixintype") >> { true }
            canAddMixin("unwritablemixin") >> { false }
        }

        final protoNodeDecorator = Spy(ProtoNodeDecorator, constructorArgs: [decoratedProtoNode]) {
            getOrCreateNode(session) >> { node }
        }

        when:
        protoNodeDecorator.writeToJcr(session)

        then:
        //Only one mixin should be valid
        1 * node.addMixin("somemixintype")
        //Only one other property that needs to be written
        1 * node.setProperty(_, _, _)
    }
}
