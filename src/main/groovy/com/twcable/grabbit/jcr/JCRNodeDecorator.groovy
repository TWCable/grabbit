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
import com.twcable.grabbit.proto.NodeProtos.Node.Builder as ProtoNodeBuilder
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import groovy.transform.CompileStatic

import groovy.util.logging.Slf4j
import org.apache.jackrabbit.value.DateValue

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.jcr.Node as JCRNode
import javax.jcr.Property
import javax.jcr.Property as JcrProperty
import javax.jcr.RepositoryException
import javax.jcr.nodetype.ItemDefinition

import static org.apache.jackrabbit.JcrConstants.*

@CompileStatic
@Slf4j
class JcrNodeDecorator {

    @Delegate
    JCRNode innerNode


    //Evaluated in a lazy fashion
    private Collection<JcrNodeDecorator> immediateChildNodes


    JcrNodeDecorator(@Nonnull JCRNode node) {
        if(!node) throw new IllegalArgumentException("node must not be null!")
        this.innerNode = node
    }


    /**
     * @return this node's immediate children, empty if none
     */
    Collection<JcrNodeDecorator> getImmediateChildNodes() {
        if(!immediateChildNodes) {
            immediateChildNodes = (getNodes().collect { JCRNode node -> new JcrNodeDecorator(node) }  ?: []) as Collection<JcrNodeDecorator>
        }
        return immediateChildNodes
    }


    void setLastModified() {
        final lastModified = new DateValue(Calendar.instance)
        try {
            //Need to check if jcr:lastModified can be added to the current node via its NodeType definition
            //as it cannot be added to all the nodes
            if (innerNode.primaryNodeType.canSetProperty(JCR_LASTMODIFIED, lastModified)) {
                innerNode.setProperty(JCR_LASTMODIFIED, lastModified)
            }
        }
        catch (RepositoryException ex) {
            log.error "Exception while setting jcr:lastModified on ${innerNode.path}.", ex
        }
    }


    String getPrimaryType() {
        innerNode.getProperty(JCR_PRIMARYTYPE).string
    }


    /**
    * Identify all required child nodes
    * @return list of immediate required child nodes that must exist with this node, or null if no children
    */
    @Nullable
    Collection<JcrNodeDecorator> getRequiredChildNodes() {
        return hasMandatoryChildNodes() ? getImmediateChildNodes().findAll{ JcrNodeDecorator childJcrNode -> childJcrNode.isRequiredNode() } : null
    }


    /**
    * Checks to see if this node has any immediate nodes that are required to be written with this node as part of its definition
    * @return true if this node has any immediate required nodes, false otherwise
    */
    boolean hasMandatoryChildNodes() {
        return primaryNodeType.childNodeDefinitions.any { ((ItemDefinition)it).mandatory }
    }


    /**
    * This node is a required node of some parent node definition
    * @return true if mandatory, false if not
    */
    boolean isRequiredNode() {
        return definition.isMandatory()
    }

    /**
     * Build node and "only" mandatory child nodes
     */
    @Nonnull
     ProtoNode toProtoNode() {
        final ProtoNodeBuilder protoNodeBuilder = ProtoNode.newBuilder()
        protoNodeBuilder.setName(path)
        protoNodeBuilder.addAllProperties(getProtoProperties())
        requiredChildNodes?.each {
            protoNodeBuilder.addMandatoryChildNode(it.toProtoNode())
        }
        return protoNodeBuilder.build()
    }

    /**
     * @return resulting collection of transferable proto properties collected from jcrNode
     */
    @Nonnull
    private Collection<ProtoProperty> getProtoProperties() {
        final List<Property> properties = properties.toList()
        return properties.findResults { JcrProperty jcrProperty ->
            JcrPropertyDecorator decoratedProperty = new JcrPropertyDecorator(jcrProperty)
            decoratedProperty.isTransferable() ? decoratedProperty.toProtoProperty() : null
        }
    }

    /**
     * Returns the "jcr:lastModified", "cq:lastModified" or "jcr:created" date property
     * for current Jcr Node
     * @return null if none of the 3 are found
     */
    @Nullable
    Date getModifiedOrCreatedDate() {
        final String CQ_LAST_MODIFIED = "cq:lastModified"
        if (hasProperty(JCR_LASTMODIFIED)) {
            return getProperty(JCR_LASTMODIFIED).date.time
        }
        else if (hasProperty(CQ_LAST_MODIFIED)) {
            return getProperty(CQ_LAST_MODIFIED).date.time
        }
        else if (hasProperty(JCR_CREATED)) {
            return getProperty(JCR_CREATED).date.time
        }
        return null
    }


    Object asType(Class clazz) {
        if(clazz == JCRNode) {
            return innerNode
        }
        else {
            super.asType(clazz)
        }
    }
}
