package com.twcable.grabbit.jcr

import com.twcable.grabbit.DateUtil
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import groovy.transform.CompileStatic
import org.apache.jackrabbit.value.ValueFactoryImpl

import javax.annotation.Nonnull
import javax.jcr.Node as JCRNode
import javax.jcr.Property
import javax.jcr.PropertyType
import javax.jcr.Value
import javax.jcr.ValueFormatException

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@CompileStatic
class ProtoPropertyDecorator {

    @Delegate
    ProtoProperty innerProtoProperty

    ProtoPropertyDecorator(@Nonnull ProtoProperty protoProperty) {
        this.innerProtoProperty = protoProperty
    }


    void writeToNode(@Nonnull JCRNode node) {
        if(primaryType || mixinType) throw new IllegalStateException("Refuse to write jcr:primaryType or jcr:mixinType as normal properties.  These are not allowed")
        try {
            if (!innerProtoProperty.hasValues()) {
                node.setProperty(innerProtoProperty.name, getPropertyValue(), innerProtoProperty.type)
            }
            else {
                Value[] values = getPropertyValues()
                node.setProperty(innerProtoProperty.name, values, innerProtoProperty.type)
            }
        }
        catch (ValueFormatException ex) {
            /**
             * We do this for the case were Grabbit attempts to write a property of a property type, different than the type already written
             * i.e String vs String[]
             */
            if (ex.message.contains("Multivalued property can not be set to a single value")) {
                //If this is the exception, that means that a property with the name already exists
                final Property currentProperty = node.getProperty(innerProtoProperty.name)
                currentProperty.remove()
                if (currentProperty.multiple) {
                    final Value[] values = [getPropertyValue()]
                    node.setProperty(innerProtoProperty.name, values, innerProtoProperty.type)
                }
            }
            else if (ex.message.contains("Single-valued property can not be set to an array of values")) {
                node.setProperty(innerProtoProperty.name, getPropertyValues().first(), innerProtoProperty.type)
            }
        }
    }


    boolean isPrimaryType() {
        innerProtoProperty.name == JCR_PRIMARYTYPE
    }


    boolean isMixinType() {
        innerProtoProperty.name == JCR_MIXINTYPES
    }


    private Value getPropertyValue() throws ValueFormatException {
        getJCRValueFromProtoValue(innerProtoProperty.getValue())
    }


    private Value[] getPropertyValues() throws ValueFormatException {
        return innerProtoProperty.values.valueList.collect { ProtoValue protoValue -> getJCRValueFromProtoValue(protoValue) } as Value[]
    }


    private Value getJCRValueFromProtoValue(ProtoValue value) throws ValueFormatException {

        final valueFactory = ValueFactoryImpl.getInstance()

        switch(innerProtoProperty.type) {
            case PropertyType.BINARY:
                final binary = valueFactory.createBinary(new ByteArrayInputStream(value.bytesValue.toByteArray()))
                return valueFactory.createValue(binary)
            case PropertyType.DATE:
                final date = DateUtil.getCalendarFromISOString(value.stringValue)
                return valueFactory.createValue(date)
            default:
                return valueFactory.createValue(value.stringValue, innerProtoProperty.type)
        }
    }
}
