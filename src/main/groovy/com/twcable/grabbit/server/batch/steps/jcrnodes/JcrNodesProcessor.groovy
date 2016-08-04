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

package com.twcable.grabbit.server.batch.steps.jcrnodes

import com.google.protobuf.ByteString
import com.twcable.grabbit.DateUtil
import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import com.twcable.grabbit.proto.NodeProtos.Node.Builder as ProtoNodeBuilder
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import com.twcable.grabbit.proto.NodeProtos.Property.Builder as ProtoPropertyBuilder
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import com.twcable.grabbit.proto.NodeProtos.Value.Builder as ProtoValueBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemProcessor

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.jcr.Node as JcrNode
import javax.jcr.Property as JcrProperty
import javax.jcr.Value
import javax.jcr.nodetype.ItemDefinition

import static javax.jcr.PropertyType.BINARY
import static org.apache.jackrabbit.JcrConstants.*

/**
 * This ItemProcessor takes javax.jcr.Node references from JcrNodesReader, and converts them into ProtoNode objects
 */
@Slf4j
@CompileStatic
class JcrNodesProcessor implements ItemProcessor<JcrNode, ProtoNode> {

    private String contentAfterDate

    void setContentAfterDate(String contentAfterDate) {
        this.contentAfterDate = contentAfterDate
    }

    /**
     * Converts a JCR Node to a {@link ProtoNode} object.
     * Returns null if current Node's processing needs to be ignored like for "rep:policy" nodes
     */
    @Override
    @Nullable
    ProtoNode process(JcrNode jcrNode) throws Exception {

        //TODO: Access Control Lists nodes are not supported right now.
        if (!jcrNode || jcrNode.path.contains("rep:policy")) {
            log.info "Ignoring current node ${jcrNode}"
            return null
        }

        if (contentAfterDate) {
            final Date afterDate = DateUtil.getDateFromISOString(contentAfterDate)
            log.debug "ContentAfterDate received : ${afterDate}. Will ignore content created or modified before the afterDate"
            final date = getDate(jcrNode)
            if (date && date.before(afterDate)) { //if there are no date properties, we treat nodes as new
                log.debug "Not sending any data older than ${afterDate}"
                return null
            }
        }

        // Skip this node because it has already been processed by its parent
        if(isMandatoryNode(jcrNode)) {
            return null;
        } else {
            // Build parent node
            return buildNode(jcrNode, getMandatoryChildNodes(jcrNode))
        }
    }

    /**
     * Build node and "only" mandatory child nodes
     * @param jcrNode
     * @param mandatoryChildNodes
     */
    @Nonnull
    private ProtoNode buildNode(JcrNode jcrNode, Collection<JcrNode> mandatoryChildNodes) {
        final ProtoNodeBuilder protoNodeBuilder = ProtoNode.newBuilder()
        protoNodeBuilder.setName(jcrNode.path)
        protoNodeBuilder.addAllProperties(getProtoPropertiesFrom(jcrNode))
        mandatoryChildNodes?.each {
            protoNodeBuilder.addMandatoryChildNode(buildNode(it, getMandatoryChildNodes(it)))
        }
        return protoNodeBuilder.build()
    }

    /**
     * Identify all required child nodes
     * @param jcrNode
     * @return Collection of required child nodes or null if jcrNode has none
     */
    @Nullable
    private static Collection<JcrNode> getMandatoryChildNodes(JcrNode jcrNode) {
        return hasMandatoryChildNodes(jcrNode) ? jcrNode.getNodes().findAll{JcrNode childJcrNode -> isMandatoryNode(childJcrNode)} : null
    }

    /**
     * Determines if a JCR Node has mandatory child nodes
     * @param jcrNode
     */
    private static boolean hasMandatoryChildNodes(JcrNode jcrNode) {
        return jcrNode.primaryNodeType.childNodeDefinitions.any { ((ItemDefinition)it).mandatory }
    }

    /**
     * Checks to see if a JCR Node is a mandatory node
     * @param node to check
     */
    private static boolean isMandatoryNode(JcrNode node) {
        return node.definition.isMandatory()
    }

    /**
     * Build a collection of Proto Properties from JCR node
     * @param jcrNode to gather properties from
     * @return resulting collection of transferable proto properties collected from jcrNode
     */
    @Nonnull
    private static Collection<ProtoProperty> getProtoPropertiesFrom(JcrNode jcrNode) {
        final List<JcrProperty> properties = jcrNode.properties.toList()
        final transferableProperties = properties.findAll { JcrProperty jcrProperty ->
            //Before adding a property to the Current Node Proto message, check if the property
            //is Valid and if it should be actually sent to the client
            isPropertyTransferable(jcrProperty)
        }
        return transferableProperties.collect { JcrProperty jcrProperty ->
            toProperty(jcrProperty)
        }
    }
    /**
     * Returns the "jcr:created", "jcr:lastModified" or "cq:lastModified" date property
     * for current Jcr Node
     * @param jcrNode
     * @return null if none of the 3 are found
     */
    @Nullable
    private static Date getDate(JcrNode jcrNode) {
        final String CQ_LAST_MODIFIED = "cq:lastModified"
        if (jcrNode.hasProperty(JCR_LASTMODIFIED)) {
            return jcrNode.getProperty(JCR_LASTMODIFIED).date.time
        }
        else if (jcrNode.hasProperty(CQ_LAST_MODIFIED)) {
            return jcrNode.getProperty(CQ_LAST_MODIFIED).date.time
        }
        else if (jcrNode.hasProperty(JCR_CREATED)) {
            return jcrNode.getProperty(JCR_CREATED).date.time
        }
        return null
    }

    /**
     * Determines if JCR Property object can be "rewritten" to the JCR. For example, we can not rewrite a node's
     * primary type; That is forbidden by the JCR spec.
     * @param jcrProperty
     */
    private static boolean isPropertyTransferable(JcrProperty jcrProperty) {
        //If property is "jcr:lastModified", we don't want to send this property to the client. If we send it, and
        //the client writes it to JCR, then we can have lastModified date for a node that is older than the creation
        //date itself
        if (jcrProperty.name == JCR_LASTMODIFIED) {
            return false
        }

        if ([JCR_PRIMARYTYPE, JCR_MIXINTYPES].contains(jcrProperty.name)) {
            return true
        }

        !jcrProperty.definition.isProtected()
    }

    /**
     * Accepts a Jcr Property and marshalls it to a ProtoProperty
     * @param jcrProperty
     */
    @Nonnull
    private static ProtoProperty toProperty(JcrProperty jcrProperty) {
        ProtoPropertyBuilder propertyBuilder = ProtoProperty.newBuilder()
        ProtoValueBuilder valueBuilder = ProtoValue.newBuilder()
        propertyBuilder.setName(jcrProperty.name)

        if(jcrProperty.type == BINARY) {
            propertyBuilder.addValues(valueBuilder.setBytesValue(ByteString.readFrom(jcrProperty.value.binary.stream)))
        }
        else {
            //Other property types can potentially have multiple values
            final Value[] values = jcrProperty.multiple ? jcrProperty.values : [jcrProperty.value] as Value[]
            values.each { Value value ->
                propertyBuilder.addValues(valueBuilder.setStringValue(value.string))
            }
        }
        propertyBuilder.setType(jcrProperty.type)
        propertyBuilder.build()
    }
}
