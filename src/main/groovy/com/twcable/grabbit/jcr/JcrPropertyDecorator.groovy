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
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import com.twcable.grabbit.proto.NodeProtos.Property.Builder as ProtoPropertyBuilder
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import com.twcable.grabbit.proto.NodeProtos.Value.Builder as ProtoValueBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import javax.annotation.Nonnull
import javax.jcr.Property as JCRProperty
import javax.jcr.Value


import static javax.jcr.PropertyType.BINARY
import static javax.jcr.PropertyType.REFERENCE
import static javax.jcr.PropertyType.WEAKREFERENCE
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@CompileStatic
@Slf4j
class JCRPropertyDecorator {

    @Delegate
    JCRProperty innerProperty

    private final JCRNodeDecorator nodeOwner


    JCRPropertyDecorator(JCRProperty property, JCRNodeDecorator nodeOwner) {
        this.innerProperty = property
        this.nodeOwner = nodeOwner
    }


    boolean isReferenceType() {
        return type == REFERENCE || type == WEAKREFERENCE
    }


    /**
     * Determines if JCR Property object can be "rewritten" to the JCR. For example, we can not rewrite a node's
     * primary type; That is forbidden by the JCR spec.
     */
    boolean isTransferable() {
        //If property is "jcr:lastModified", we don't want to send this property to the client. If we send it, and
        //the client writes it to JCR, then we can have lastModified date for a node that is older than the creation
        //date itself
        if (name == JCR_LASTMODIFIED) {
            return false
        }

        if ([JCR_PRIMARYTYPE, JCR_MIXINTYPES].contains(name)) {
            return true
        }

        if(nodeOwner.isAuthorizableType() || nodeOwner.isACType()) {
            return true
        }

        return !definition.isProtected()
    }


    @Override
    Value[] getValues() {
        return (multiple ? innerProperty.values : [innerProperty.value]) as Value[]
    }


    /**
     * Marshalls current Jcr Property to a ProtoProperty
     */
    @Nonnull
    ProtoProperty toProtoProperty() {
        ProtoPropertyBuilder propertyBuilder = ProtoProperty.newBuilder()
        ProtoValueBuilder valueBuilder = ProtoValue.newBuilder()
        propertyBuilder.setName(name)

        if(type == BINARY) {
            propertyBuilder.addValues(valueBuilder.setBytesValue(ByteString.readFrom(value.binary.stream)))
        }
        else {
            //Other property types can potentially have multiple values
            values.each { Value value ->
                propertyBuilder.addValues(valueBuilder.setStringValue(value.string))
            }
        }
        propertyBuilder.setMultiple(multiple)
        propertyBuilder.setType(type)
        propertyBuilder.build()
    }
}
