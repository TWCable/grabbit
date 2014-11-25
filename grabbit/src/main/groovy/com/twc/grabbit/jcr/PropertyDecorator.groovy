package com.twc.grabbit.jcr

import com.twc.grabbit.DateUtil
import com.twc.grabbit.proto.NodeProtos
import groovy.transform.CompileStatic
import org.apache.jackrabbit.value.ValueFactoryImpl

import javax.annotation.Nonnull
import javax.jcr.PropertyType
import javax.jcr.Value
import javax.jcr.ValueFormatException

@CompileStatic
class PropertyDecorator {

    @Delegate
    NodeProtos.Property protoProperty

    private PropertyDecorator(NodeProtos.Property protoProperty) {
        this.protoProperty = protoProperty
    }

    static PropertyDecorator from(@Nonnull NodeProtos.Property protoProperty) {
        if(protoProperty == null) throw new IllegalArgumentException("Property can not be null!")
        return new PropertyDecorator(protoProperty)
    }

    Value getPropertyValue() throws ValueFormatException {
        getJCRValueFromProtoValue(value)
    }

    Value[] getPropertyValues() throws ValueFormatException {
        return values.valueList.collect { NodeProtos.Value protoValue -> getJCRValueFromProtoValue(protoValue)} as Value[]
    }

    private Value getJCRValueFromProtoValue(NodeProtos.Value value) throws ValueFormatException {

        final valueFactory = ValueFactoryImpl.getInstance()

        if(protoProperty.type == PropertyType.BINARY) {
            final binary = valueFactory.createBinary(new ByteArrayInputStream(value.bytesValue.toByteArray()))
            return valueFactory.createValue(binary)
        }
        else if(protoProperty.type == PropertyType.DATE) {
            final date = DateUtil.getCalendarFromISOString(value.stringValue)
            return valueFactory.createValue(date)
        }

        return valueFactory.createValue(value.stringValue, protoProperty.type)
    }
}
