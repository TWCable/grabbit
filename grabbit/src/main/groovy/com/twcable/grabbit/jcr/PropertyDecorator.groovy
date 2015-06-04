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

import com.twcable.grabbit.DateUtil
import com.twcable.grabbit.proto.NodeProtos
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
        if (protoProperty == null) throw new IllegalArgumentException("Property can not be null!")
        return new PropertyDecorator(protoProperty)
    }


    Value getPropertyValue() throws ValueFormatException {
        getJCRValueFromProtoValue(value)
    }


    Value[] getPropertyValues() throws ValueFormatException {
        return values.valueList.collect { NodeProtos.Value protoValue -> getJCRValueFromProtoValue(protoValue) } as Value[]
    }


    private Value getJCRValueFromProtoValue(NodeProtos.Value value) throws ValueFormatException {

        final valueFactory = ValueFactoryImpl.getInstance()

        if (protoProperty.type == PropertyType.BINARY) {
            final binary = valueFactory.createBinary(new ByteArrayInputStream(value.bytesValue.toByteArray()))
            return valueFactory.createValue(binary)
        }
        else if (protoProperty.type == PropertyType.DATE) {
            final date = DateUtil.getCalendarFromISOString(value.stringValue)
            return valueFactory.createValue(date)
        }

        return valueFactory.createValue(value.stringValue, protoProperty.type)
    }
}
