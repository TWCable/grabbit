package com.twc.grabbit.jcr

import com.google.protobuf.ByteString
import com.twc.grabbit.proto.NodeProtos

import javax.jcr.PropertyType

class ProtoMock {

    static getSingleValueProperty(final int type = PropertyType.STRING, final String value = "value") {
        return NodeProtos.Property.newBuilder().
            setName("name").
            setType(type).
            setValue(NodeProtos.Value.newBuilder().
                setStringValue(value).
                setBytesValue(ByteString.copyFrom(value.bytes))
        ).build()
    }

    static getMultiValuedProperty(final int type = PropertyType.STRING, final String... values = ["value1", "value2"] ) {

        final theValues = values.collect { String value ->
            NodeProtos.Value.newBuilder().
                setStringValue(value).
                setBytesValue(ByteString.copyFrom(value.bytes)).build()
        }
        return NodeProtos.Property.newBuilder().
            setName("name").
            setType(type).
            setValues(NodeProtos.Values.newBuilder().
                addAllValue(theValues)
        ).build()
    }
}
