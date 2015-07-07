package com.twcable.grabbit.jcr

import com.day.cq.commons.jcr.JcrConstants
import com.google.protobuf.ByteString
import com.twcable.grabbit.proto.NodeProtos.Property as PropertyProto
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import com.twcable.grabbit.proto.NodeProtos.Values as ProtoValues
import org.apache.jackrabbit.value.BinaryValue
import org.apache.jackrabbit.value.DateValue
import org.apache.jackrabbit.value.StringValue
import spock.lang.Specification

import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.PropertyType
import javax.jcr.Value
import javax.jcr.ValueFormatException

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
                                        .setValue(
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
                                            .setName("someproperty")
                                            .setType(PropertyType.STRING)
                                            .setValue(
                                                ProtoValue.newBuilder()
                                                    .setStringValue("somevalue")
                                                    .build()
                                            )
                                            .build()

        final propertyDecorator = new ProtoPropertyDecorator(someProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        1 * mockNode.setProperty("someproperty", new StringValue("somevalue"), PropertyType.STRING)
    }


    def "A multi-value property can be written correctly to a node that expects a multi-value property"() {
        given:
        final mockNode = Mock(Node)

        when:
        PropertyProto someMultiProperty = PropertyProto.newBuilder()
                                              .setName("somemultivalueproperty")
                                              .setType(PropertyType.STRING)
                                              .setValues(
                                                  ProtoValues.newBuilder()
                                                      .addAllValue(
                                                          [
                                                              ProtoValue.newBuilder().setStringValue("value1").build(),
                                                              ProtoValue.newBuilder().setStringValue("value2").build()
                                                          ]
                                                      )
                                                      .build()
                                              )
                                              .build()

        final propertyDecorator = new ProtoPropertyDecorator(someMultiProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        1 * mockNode.setProperty("somemultivalueproperty", [new StringValue("value1"), new StringValue("value2")] as Value[], PropertyType.STRING)
    }


    def "A single-value property that is attempted to be written to a multi-value property can recover, and be written"() {
        given:
        final mockNode = Mock(Node)

        when:
        PropertyProto someProperty = PropertyProto.newBuilder()
                                              .setName("somename")
                                              .setType(PropertyType.STRING)
                                              .setValue(
                                                  ProtoValue.newBuilder()
                                                      .setStringValue("somevalue")
                                                      .build()
                                              )
                                              .build()

        mockNode.setProperty("somename", new StringValue("somevalue"), PropertyType.STRING) >> { throw new ValueFormatException("Multivalued property can not be set to a single value") }
        mockNode.getProperty("somename") >> {
            Mock(Property) {
                isMultiple() >> true
            }
        }

        final propertyDecorator = new ProtoPropertyDecorator(someProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        notThrown(ValueFormatException)
        1 * mockNode.setProperty("somename", [new StringValue("somevalue")] as Value[], PropertyType.STRING)
    }


    def "A multi-value property that is attempted to be written to a single-value property can recover, and be written"() {
        given:
        final mockNode = Mock(Node)

        when:
        PropertyProto someMultiProperty = PropertyProto.newBuilder()
                                                .setName("somemultivalueproperty")
                                                .setType(PropertyType.STRING)
                                                .setValues(
                                                    ProtoValues.newBuilder()
                                                        .addAllValue(
                                                            [
                                                                ProtoValue.newBuilder().setStringValue("value1").build(),
                                                                ProtoValue.newBuilder().setStringValue("value2").build()
                                                            ]
                                                        )
                                                        .build()
                                                )
                                                .build()

        mockNode.setProperty("somemultivalueproperty", [new StringValue("value1"), new StringValue("value2")] as Value[], PropertyType.STRING) >> { throw new ValueFormatException("Single-valued property can not be set to an array of values") }

        final propertyDecorator = new ProtoPropertyDecorator(someMultiProperty)
        propertyDecorator.writeToNode(mockNode)

        then:
        notThrown(ValueFormatException)
        1 * mockNode.setProperty("somemultivalueproperty", new StringValue("value1"), PropertyType.STRING)
    }


    def "A property with a binary value can be successfully written"() {
        given:
        final mockNode = Mock(Node)


        when:
        PropertyProto someProperty = PropertyProto.newBuilder()
                                        .setName(JcrConstants.JCR_DATA)
                                        .setType(PropertyType.BINARY)
                                        .setValue(
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
                                        .setValue(
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
