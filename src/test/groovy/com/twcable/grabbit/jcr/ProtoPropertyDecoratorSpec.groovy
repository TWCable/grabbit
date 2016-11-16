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
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.PropertyType
import javax.jcr.Session
import javax.jcr.Value
import javax.jcr.ValueFormatException
import org.apache.jackrabbit.value.BinaryValue
import org.apache.jackrabbit.value.DateValue
import org.apache.jackrabbit.value.StringValue
import spock.lang.Specification


import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.NT_REP_ACL
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.NT_REP_GRANT_ACE

class ProtoPropertyDecoratorSpec extends Specification {

    def "When a property is either jcr:primaryType or jcr:mixinTypes, decorator will refuse to write these nodes are regular properties"() {
        given:
        final mockNode = Mock(Node)

        when:
        ProtoProperty someProperty = ProtoProperty.newBuilder()
                                        .setName(refusedType)
                                        .setType(PropertyType.STRING)
                                        .setMultiple(false)
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
        ProtoProperty someProperty = ProtoProperty.newBuilder()
                                            .setName("property")
                                            .setType(PropertyType.STRING)
                                            .setMultiple(false)
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
        ProtoProperty someMultiProperty = ProtoProperty.newBuilder()
                                              .setName("property")
                                              .setType(PropertyType.STRING)
                                              .setMultiple(true)
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
        ProtoProperty someMultiProperty = ProtoProperty.newBuilder()
                                            .setName("property")
                                            .setMultiple(true)
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

        ProtoProperty someProperty = ProtoProperty.newBuilder()
                                        .setName("property")
                                        .setType(PropertyType.STRING)
                                        .setMultiple(false)
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

        ProtoProperty someMultiProperty = ProtoProperty.newBuilder()
                                                .setName("property")
                                                .setType(PropertyType.STRING)
                                                .setMultiple(true)
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
        ProtoProperty someProperty = ProtoProperty.newBuilder()
                                        .setName(JcrConstants.JCR_DATA)
                                        .setType(PropertyType.BINARY)
                                        .setMultiple(false)
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
        ProtoProperty someProperty = ProtoProperty.newBuilder()
                                        .setName("somedate")
                                        .setType(PropertyType.DATE)
                                        .setMultiple(false)
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

    def "isUserType()"() {
        when:
        ProtoProperty property = ProtoProperty.newBuilder()
                                    .setName(name)
                                    .setType(PropertyType.NAME)
                                    .setMultiple(false)
                                    .addValues(
                                        ProtoValue.newBuilder()
                                            .setStringValue(value)
                                            .build()
                                    )
                                    .build()


        final ProtoPropertyDecorator propertyDecorator = new ProtoPropertyDecorator(property)

        then:
        propertyDecorator.isUserType() == expectedValue

        where:
        name             |   value       |  expectedValue
        JCR_PRIMARYTYPE  |   'rep:User'  |  true
        JCR_PRIMARYTYPE  |   'rep:Group' |  false
        'propertyname'   |   'rep:User'  |  false
    }

    def "isGroupType()"() {
        when:
        ProtoProperty property = ProtoProperty.newBuilder()
                                    .setName(name)
                                    .setType(PropertyType.NAME)
                                    .setMultiple(false)
                                    .addValues(
                                        ProtoValue.newBuilder()
                                            .setStringValue(value)
                                            .build()
                                    )
                                    .build()


        final ProtoPropertyDecorator propertyDecorator = new ProtoPropertyDecorator(property)

        then:
        propertyDecorator.isGroupType() == expectedValue

        where:
        name             |   value       |  expectedValue
        JCR_PRIMARYTYPE  |   'rep:User'  |  false
        JCR_PRIMARYTYPE  |   'rep:Group' |  true
        'propertyname'   |   'rep:Group' |  false
    }

    def "isAuthorizableIDType()"() {
        when:
        ProtoProperty property = ProtoProperty.newBuilder()
                .setName(name)
                .setType(PropertyType.NAME)
                .setMultiple(false)
                .addValues(
                ProtoValue.newBuilder()
                        .setStringValue('somevalue')
                        .build()
        )
                .build()


        final ProtoPropertyDecorator propertyDecorator = new ProtoPropertyDecorator(property)

        then:
        propertyDecorator.isAuthorizableIDType() == expectedValue

        where:
        name                 |   expectedValue
        'rep:authorizableId' |   true
        'propertyname'       |   false
    }

    def "isACType()"() {
        when:
        ProtoProperty property = ProtoProperty.newBuilder()
                                    .setName(name)
                                    .setType(PropertyType.NAME)
                                    .setMultiple(false)
                                    .addValues(
                                        ProtoValue.newBuilder()
                                                .setStringValue(value)
                                                .build()
                                    )
                                    .build()


        final ProtoPropertyDecorator propertyDecorator = new ProtoPropertyDecorator(property)

        then:
        propertyDecorator.isACType() == expectedValue

        where:
        name             |   value             |  expectedValue
        JCR_PRIMARYTYPE  |   NT_REP_ACL        |  true
        JCR_PRIMARYTYPE  |   NT_REP_GRANT_ACE  |  true
        'propertyname'   |   NT_REP_ACL        |  false
    }
}
