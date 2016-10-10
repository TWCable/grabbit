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
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.value.ValueFactoryImpl

import javax.annotation.Nonnull
import javax.jcr.Node as JCRNode
import javax.jcr.PropertyType
import javax.jcr.Value
import javax.jcr.ValueFormatException

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@CompileStatic
@Slf4j
class ProtoPropertyDecorator {

    @Delegate
    ProtoProperty innerProtoProperty


    ProtoPropertyDecorator(@Nonnull ProtoProperty protoProperty) {
        this.innerProtoProperty = protoProperty
    }


    void writeToNode(@Nonnull JCRNode node) {
        if(primaryType || mixinType) throw new IllegalStateException("Refuse to write jcr:primaryType or jcr:mixinType as normal properties.  These are not allowed")
        try {
            if (multiple) {
                node.setProperty(this.name, getPropertyValues(), this.type)
            }
            else {
                node.setProperty(this.name, getPropertyValue(), this.type)
            }
        }
        catch (ValueFormatException ex) {
            //We do this for the case were Grabbit attempts to write a property of a type different from the type already written i.e String vs String[]
            //Get the problem property already set on the node
            final existingProperty = node.getProperty(name)

            log.debug "Failure initially setting property ${name} with type: ${PropertyType.nameFromValue(type)}${multiple ? '[]' : ''} to existing property with type: ${PropertyType.nameFromValue(existingProperty.type)}${existingProperty.multiple ? '[]' : ''}.  Trying to resolve..."
            //If the type is different than what we expect to write, or the cardinality is different; remove what is already written, and retry
            if(existingProperty.type != this.type || existingProperty.multiple ^ this.multiple) {
                existingProperty.remove()
                node.session.save()
                this.writeToNode(node)
                log.debug "Resolve successful..."
            }
            else {
                log.warn "WARNING!  Property ${name} will not be written to ${node.name}!  There was a problem when writing value type ${PropertyType.nameFromValue(type)}${multiple ? '[]' : ''} to existing node with same type, due to a ValueFormatException, and we were unable to recover"
            }
        }
    }


    boolean isPrimaryType() {
        innerProtoProperty.name == JCR_PRIMARYTYPE
    }


    boolean isMixinType() {
        innerProtoProperty.name == JCR_MIXINTYPES
    }


    boolean isUserType() {
        getStringValue() == 'rep:User'
    }


    boolean isGroupType() {
        getStringValue() == 'rep:Group'
    }


    boolean isAuthorizableIDType() {
        innerProtoProperty.name == 'rep:authorizableId'
    }


    String getStringValue() {
        getValue().stringValue
    }


    private ProtoValue getValue() {
        innerProtoProperty.valuesList.first()
    }


    private Value getPropertyValue() throws ValueFormatException {
        getJCRValueFromProtoValue(getValue())
    }


    private Value[] getPropertyValues() throws ValueFormatException {
        return innerProtoProperty.valuesList.collect { ProtoValue protoValue -> getJCRValueFromProtoValue(protoValue) } as Value[]
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
