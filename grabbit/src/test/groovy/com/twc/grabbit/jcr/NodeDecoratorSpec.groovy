package com.twc.grabbit.jcr

import com.twc.grabbit.proto.NodeProtos
import org.apache.jackrabbit.value.StringValue
import spock.lang.Specification

import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.PropertyType
import javax.jcr.ValueFormatException

import static com.twc.grabbit.jcr.ProtoMock.getMultiValuedProperty
import static com.twc.grabbit.jcr.ProtoMock.getSingleValueProperty

@SuppressWarnings("GroovyAssignabilityCheck")
class NodeDecoratorSpec extends Specification {

    def "Attempting to create a node decorator with a null node results in an exception"() {
        when:
        NodeDecorator.from(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "setProperty() with a single value set to a node expecting single values works as expected"() {
        setup:
        Node node = Mock(Node)

        NodeProtos.Property property = getSingleValueProperty()

        PropertyDecorator propertyDecorator = PropertyDecorator.from(property)

        when:
        NodeDecorator.from(node).setProperty(propertyDecorator)

        then:
        1 * node.setProperty("name", new StringValue("value"), PropertyType.STRING)
    }

    def "setProperty() with a single value set to a node expecting multiple values works as expected"() {
        setup:
        Node node = Mock(Node)

        node.setProperty("name", new StringValue("value"), PropertyType.STRING) >> {
            throw new ValueFormatException("Multivalued property can not be set to a single value")
        }

        node.getProperty("name") >> {
            Property property = Mock(Property)
            property.isMultiple() >> true
            return property
        }

        NodeProtos.Property property = getSingleValueProperty()

        PropertyDecorator propertyDecorator = PropertyDecorator.from(property)

        when:
        NodeDecorator.from(node).setProperty(propertyDecorator)

        then:
        1 * node.setProperty("name", [new StringValue("value")], PropertyType.STRING)

    }

    def "setProperty() with a multiple value set to a node expecting multiple values works as expected"() {
        setup:
        Node node = Mock(Node)

        NodeProtos.Property property = getMultiValuedProperty()

        PropertyDecorator propertyDecorator = PropertyDecorator.from(property)

        when:
        NodeDecorator.from(node).setProperty(propertyDecorator)

        then:
        1 * node.setProperty("name", [new StringValue("value1"), new StringValue("value2")], PropertyType.STRING)
    }

    def "setProperty() with a multiple value set to a node expecting a single value works as expected"() {
        setup:
        Node node = Mock(Node)

        node.setProperty("name", [new StringValue("value1"), new StringValue("value2")], PropertyType.STRING) >> {
            throw new ValueFormatException("Single-valued property can not be set to an array of values")
        }

        NodeProtos.Property property = getMultiValuedProperty()

        PropertyDecorator propertyDecorator = PropertyDecorator.from(property)

        when:
        NodeDecorator.from(node).setProperty(propertyDecorator)

        then:
        1 * node.setProperty("name", new StringValue("value1"), PropertyType.STRING)
    }
}
