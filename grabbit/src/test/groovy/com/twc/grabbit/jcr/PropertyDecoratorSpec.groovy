package com.twc.grabbit.jcr

import com.twc.grabbit.proto.NodeProtos
import org.apache.jackrabbit.value.*
import spock.lang.Specification

import javax.jcr.PropertyType
import javax.jcr.Value

import static com.twc.grabbit.jcr.ProtoMock.getMultiValuedProperty
import static com.twc.grabbit.jcr.ProtoMock.getSingleValueProperty

@SuppressWarnings("GroovyAssignabilityCheck")
class PropertyDecoratorSpec extends Specification {

    def "An exception is thrown when a null property is used to construct a PropertyDecorator"() {
        when:
        PropertyDecorator.from(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "getPropertyValue() works as expected"() {
        when:
        NodeProtos.Property property = getSingleValueProperty(type, value)

        PropertyDecorator propertyDecorator = PropertyDecorator.from(property)

        Value returnValue = propertyDecorator.getPropertyValue()

        then:
        returnValue?.class == clazz

        where:
        type                   | clazz          | value
        PropertyType.STRING    | StringValue    | "value"
        PropertyType.NAME      | NameValue      | "value"
        PropertyType.URI       | URIValue       | "value"
        PropertyType.BINARY    | BinaryValue    | "value"
        PropertyType.BOOLEAN   | BooleanValue   | "true"
        PropertyType.DATE      | DateValue      | "2012-01-10"
        PropertyType.DECIMAL   | DecimalValue   | "2.1"
        PropertyType.DOUBLE    | DoubleValue    | "2.1"
        PropertyType.LONG      | LongValue      | "2"
    }

    def "getPropertyValues() works as expected"() {
        when:
        NodeProtos.Property property = getMultiValuedProperty(type, value)

        PropertyDecorator propertyDecorator = PropertyDecorator.from(property)

        Value[] returnValues = propertyDecorator.getPropertyValues()

        then:
        returnValues.each {
            it?.class == clazz
        }

        where:
        type                   | clazz          | value
        PropertyType.STRING    | StringValue    | "value"
        PropertyType.NAME      | NameValue      | "value"
        PropertyType.URI       | URIValue       | "value"
        PropertyType.BINARY    | BinaryValue    | "value"
        PropertyType.BOOLEAN   | BooleanValue   | "true"
        PropertyType.DATE      | DateValue      | "2012-01-10"
        PropertyType.DECIMAL   | DecimalValue   | "2.1"
        PropertyType.DOUBLE    | DoubleValue    | "2.1"
        PropertyType.LONG      | LongValue      | "2"
    }

}
