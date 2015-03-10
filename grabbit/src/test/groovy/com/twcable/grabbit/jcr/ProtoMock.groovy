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

import com.google.protobuf.ByteString
import com.twcable.grabbit.proto.NodeProtos

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
