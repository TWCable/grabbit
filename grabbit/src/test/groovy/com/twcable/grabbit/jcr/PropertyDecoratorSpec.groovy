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
import org.apache.jackrabbit.value.*
import spock.lang.Specification

import javax.jcr.PropertyType
import javax.jcr.Value

import static ProtoMock.getMultiValuedProperty
import static ProtoMock.getSingleValueProperty

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
