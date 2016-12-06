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

import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import javax.annotation.Nonnull
import javax.jcr.Node as JCRNode
import javax.jcr.Session
import javax.jcr.Value
import org.apache.jackrabbit.commons.JcrUtils
import org.apache.jackrabbit.value.StringValue


import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@CompileStatic
@Slf4j
class DefaultProtoNodeDecorator extends ProtoNodeDecorator {


    protected DefaultProtoNodeDecorator(@Nonnull ProtoNode node, @Nonnull Collection<ProtoPropertyDecorator> protoProperties, String nameOverride) {
        this.innerProtoNode = node
        this.protoProperties = protoProperties
        this.nameOverride = nameOverride
    }


    @Override
    protected JCRNodeDecorator writeNode(@Nonnull Session session) {
        final jcrNode = getOrCreateNode(session)
        //Write mixin types first to avoid InvalidConstraintExceptions
        final mixinProperty = getMixinProperty()
        if(mixinProperty) {
            addMixins(mixinProperty, jcrNode)
        }

        final Collection<ProtoPropertyDecorator> referenceTypeProperties = writableProperties.findAll { it.isReferenceType() }
        final Collection<ProtoPropertyDecorator> nonReferenceTypeProperties = writableProperties.findAll { !it.isReferenceType() }

        //These can be written now. Reference properties can be written after we write the referenced nodes
        nonReferenceTypeProperties.each { it.writeToNode(jcrNode) }

        final Collection<JCRNodeDecorator> referenceables = writeMandatoryPieces(session, getName()).findAll { it.isReferenceable() }

        /*
         * Nodes that are referenced from reference properties are written above in writeMandatoryPieces(). We can now map each
         * reference pointer to a transferred id from a node above, and write the pointer with a mapped nodes new identifier.
         * The transferred id is what the identifier was on sending server, and the current identifier is what it is now that it is
          * written to this server. Identifiers only apply to referenceable nodes (i.e nodes with mix:referenceable)
         */
        referenceTypeProperties.each { ProtoPropertyDecorator property ->
            final Collection<Value> newReferenceIDValues = property.getStringValues().findResults { String referenceID ->
                final JCRNodeDecorator match = referenceables.find { it.transferredID == referenceID }
                if(match) {
                    return new StringValue(match.getIdentifier())
                }
            } as Collection<Value>
            if(!newReferenceIDValues.isEmpty()) {
                property.writeToNode(jcrNode, (newReferenceIDValues.toArray() as Value[]))
            }
        }

        return new JCRNodeDecorator(jcrNode, getID())
    }


    private ProtoPropertyDecorator getMixinProperty() {
        protoProperties.find { it.isMixinType() }
    }


    private Collection<ProtoPropertyDecorator> getWritableProperties() {
        protoProperties.findAll { !(it.name in [JCR_PRIMARYTYPE, JCR_MIXINTYPES]) }
    }

    /**
     * This method is rather succinct, but helps isolate this JcrUtils static method call
     * so that we can get better test coverage.
     * @param session to create or get the node path for
     * @return the newly created, or found node
     */
    JCRNode getOrCreateNode(Session session) {
        JcrUtils.getOrCreateByPath(getName(), primaryType.getStringValue(), session)
    }


    /**
     * If a property can be added as a mixin, adds it to the given node
     * @param property
     * @param node
     */
    private static void addMixins(ProtoPropertyDecorator property, JCRNode node) {
        property.valuesList.each { ProtoValue value ->
            if (node.canAddMixin(value.stringValue)) {
                node.addMixin(value.stringValue)
                log.debug "Added mixin ${value.stringValue} for : ${node.name}."
            }
            else {
                log.warn "Encountered invalid mixin type while unmarshalling for Proto value : ${value}"
            }
        }
    }

}
