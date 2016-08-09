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
import com.google.protobuf.ByteString
import com.twcable.grabbit.proto.NodeProtos.Property as PropertyProto
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import org.apache.jackrabbit.value.BinaryValue
import org.apache.jackrabbit.value.DateValue
import org.apache.jackrabbit.value.StringValue
import spock.lang.Specification

import javax.jcr.*

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

class ProtoPropertyDecoratorSpec extends Specification {

    def "When a property is either jcr:primaryType or jcr:mixinTypes, decorator will refuse to write these nodes are regular properties"() {
        given:
        final mockNode = Mock(Node)

        when:
        PropertyProto someProperty = PropertyProto.newBuilder()
                                        .setName(refusedType)
                                        .setType(PropertyType.STRING)
                                        .addValues(
                                            ProtoValue.newBuilder()
                                                .setStringValue("somevalue")
                                                .build()
                                        )
                                        .build()

        final propertyDecorator = new ProtoPropertyDecorator(someProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        thrown(IllegalStateException)

        where:
        refusedType << [JCR_PRIMARYTYPE, JCR_MIXINTYPES]
    }


    def "A single-value property can be written correctly to a node that expects a single-value property"() {
        given:
        final mockNode = Mock(Node)

        when:
        PropertyProto someProperty = PropertyProto.newBuilder()
                                            .setName("property")
                                            .setType(PropertyType.STRING)
                                            .addValues(
                                                ProtoValue.newBuilder()
                                                    .setStringValue("somevalue")
                                                    .build()
                                            )
                                            .build()

        final propertyDecorator = new ProtoPropertyDecorator(someProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        1 * mockNode.setProperty("property", new StringValue("somevalue"), PropertyType.STRING)
    }


    def "A multi-value property can be written correctly to a node that expects a multi-value property"() {
        given:
        final mockNode = Mock(Node)

        when:
        PropertyProto someMultiProperty = PropertyProto.newBuilder()
                                              .setName("property")
                                              .setType(PropertyType.STRING)
                                              .addAllValues(
                                                  [
                                                      ProtoValue.newBuilder().setStringValue("value1").build(),
                                                      ProtoValue.newBuilder().setStringValue("value2").build()
                                                  ]
                                              )
                                              .build()

        final propertyDecorator = new ProtoPropertyDecorator(someMultiProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        1 * mockNode.setProperty("property", [new StringValue("value1"), new StringValue("value2")] as Value[], PropertyType.STRING)
    }

    def "A multi-value property field with no values, can be written correctly to a node that expects a multi-value property"() {
        given:
        final mockNode = Mock(Node)

        when:
        PropertyProto someMultiProperty = PropertyProto.newBuilder()
                                            .setName("property")
                                            .setType(PropertyType.STRING).build()

        final propertyDecorator = new ProtoPropertyDecorator(someMultiProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        1 * mockNode.setProperty("property", [] as Value[], PropertyType.STRING)
    }


    def "A single-value property that is attempted to be written to a multi-value property can recover, and be written"() {
        when:
        final mockNode = Mock(Node) {
            //Initially, the property value will be of the wrong type, so the jcr will throw a ValueFormatException.  The proceeding call, we should have resolved the problem
            setProperty("property", new StringValue("somevalue"), PropertyType.STRING) >> {
                throw new ValueFormatException("Multivalued property can not be set to a single value")
            } >> Mock(Property)
            getProperty("property") >> {
                Mock(Property) {
                    1 * remove()
                    isMultiple() >> true
                    getType() >> PropertyType.STRING
                }
            }
            getSession() >> {
                Mock(Session) {
                    1 * save()
                }
            }
        }

        PropertyProto someProperty = PropertyProto.newBuilder()
                                        .setName("property")
                                        .setType(PropertyType.STRING)
                                        .addValues(
                                            ProtoValue.newBuilder()
                                                .setStringValue("somevalue")
                                                .build()
                                        )
                                        .build()

        final propertyDecorator = new ProtoPropertyDecorator(someProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        notThrown(ValueFormatException)
    }


    def "A multi-value property that is attempted to be written to a single-value property can recover, and be written"() {
        when:
        final mockNode = Mock(Node) {
            //Initially, the property value will be of the wrong type, so the jcr will throw a ValueFormatException.  The proceeding call, we should have resolved the problem
            setProperty("property", [new StringValue("value1"), new StringValue("value2")] as Value[], PropertyType.STRING) >> {
                throw new ValueFormatException("Single-valued property can not be set to an array of values")
            } >> Mock(Property)
            getProperty("property") >> {
                Mock(Property) {
                    1 * remove()
                    isMultiple() >> false
                    getType() >> PropertyType.STRING
                }
            }
            getSession() >> {
                Mock(Session) {
                    1 * save()
                }
            }
        }

        PropertyProto someMultiProperty = PropertyProto.newBuilder()
                                                .setName("property")
                                                .setType(PropertyType.STRING)
                                                .addAllValues(
                                                    [
                                                        ProtoValue.newBuilder().setStringValue("value1").build(),
                                                        ProtoValue.newBuilder().setStringValue("value2").build()
                                                    ]
                                                )
                                                .build()


        final propertyDecorator = new ProtoPropertyDecorator(someMultiProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        notThrown(ValueFormatException)
    }


    def "A property with a binary value can be successfully written"() {
        given:
        final mockNode = Mock(Node)


        when:
        PropertyProto someProperty = PropertyProto.newBuilder()
                                        .setName(JcrConstants.JCR_DATA)
                                        .setType(PropertyType.BINARY)
                                        .addValues(
                                            ProtoValue.newBuilder()
                                                .setBytesValue(ByteString.copyFrom("somebytes".bytes))
                                            .build()
                                        )
                                        .build()


        final propertyDecorator = new ProtoPropertyDecorator(someProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        1 * mockNode.setProperty(JcrConstants.JCR_DATA, _ as BinaryValue, PropertyType.BINARY)
    }


    def "A property with a date value can be successfully written"() {
        given:
        final mockNode = Mock(Node)


        when:
        PropertyProto someProperty = PropertyProto.newBuilder()
                                        .setName("somedate")
                                        .setType(PropertyType.DATE)
                                        .addValues(
                                            ProtoValue.newBuilder()
                                                .setStringValue("2015-07-06")
                                                .build()
                                        )
                                        .build()


        final propertyDecorator = new ProtoPropertyDecorator(someProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        1 * mockNode.setProperty("somedate", _ as DateValue, PropertyType.DATE)
    }
}
