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
import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.jcr.ItemNotFoundException
import javax.jcr.Node as JCRNode
import javax.jcr.PathNotFoundException
import javax.jcr.Property as JcrProperty
import javax.jcr.RepositoryException
import javax.jcr.nodetype.ItemDefinition
import org.apache.jackrabbit.commons.flat.TreeTraverser
import org.apache.jackrabbit.value.DateValue


import static org.apache.jackrabbit.JcrConstants.JCR_CREATED
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import static org.apache.jackrabbit.commons.flat.TreeTraverser.ErrorHandler
import static org.apache.jackrabbit.commons.flat.TreeTraverser.InclusionPolicy
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.AC_NODETYPE_NAMES

@CompileStatic
@Slf4j
class JCRNodeDecorator {

    @Delegate
    JCRNode innerNode

    private final Collection<JcrPropertyDecorator> properties

    //Evaluated in a lazy fashion
    private Collection<JCRNodeDecorator> immediateChildNodes
    private List<JCRNodeDecorator> childNodeList


    JCRNodeDecorator(@Nonnull JCRNode node) {
        if(!node) throw new IllegalArgumentException("node must not be null!")
        this.innerNode = node
        Collection<JcrProperty> innerProperties = node.properties.toList()
        this.properties = innerProperties.collect { JcrProperty property ->
            new JcrPropertyDecorator(property, this)
        }
    }


    /**
     * @return this node's immediate children, empty if none
     */
    Collection<JCRNodeDecorator> getImmediateChildNodes() {
        if(!immediateChildNodes) {
            immediateChildNodes = (getNodes().collect { JCRNode node -> new JCRNodeDecorator(node) }  ?: []) as Collection<JCRNodeDecorator>
        }
        return immediateChildNodes
    }


    List<JCRNodeDecorator> getChildNodeList() {
        if(!childNodeList) {
            childNodeList = (getChildNodeIterator().collect { JCRNode node -> new JCRNodeDecorator(node) } ?: []) as List<JCRNodeDecorator>
        }
        return childNodeList
    }


    Iterator<JCRNode> getChildNodeIterator() {
        return TreeTraverser.nodeIterator(innerNode, ErrorHandler.IGNORE, new NoRootInclusionPolicy(this))
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


    /**
    * Identify all required child nodes
    * @return list of immediate required child nodes that must be transported with this node, or an empty collection if no required nodes
    */
    @Nullable
    Collection<JCRNodeDecorator> getRequiredChildNodes() {
        if(isAuthorizableType()){
            return getChildNodeList().findAll { JCRNodeDecorator childJcrNode -> !childJcrNode.isLoginToken() && !childJcrNode.isACType() }
        }
        return getMandatoryChildren()
    }


    /**
     * Some nodes must be saved together, per node definition
     */
    @Nonnull
    private Collection<JCRNodeDecorator> getMandatoryChildren() {
        return hasMandatoryChildNodes() ? getImmediateChildNodes().findAll{ JCRNodeDecorator childJcrNode -> childJcrNode.isMandatoryNode() } : []
    }


    /**
    * Checks to see if this node has any immediate nodes that are required to be written with this node as part of its definition
    * @return true if this node has any immediate required nodes, false otherwise
    */
    boolean hasMandatoryChildNodes() {
        return primaryNodeType.childNodeDefinitions.any { ((ItemDefinition)it).mandatory }
    }


    /**
    * This node is a mandatory required node of some parent node definition
    * @return true if mandatory, false if not
    */
    boolean isMandatoryNode() {
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
        requiredChildNodes.each {
            protoNodeBuilder.addMandatoryChildNode(it.toProtoNode())
        }
        return protoNodeBuilder.build()
    }

    /**
     * @return resulting collection of transferable proto properties collected from jcrNode
     */
    @Nonnull
    private Collection<ProtoProperty> getProtoProperties() {
        final Collection<JcrPropertyDecorator> transferableProperties = properties.findAll{ it.isTransferable() }
        return transferableProperties.collect{ it.toProtoProperty() }
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


    /**
     * Authorizable nodes can be unique from server to server, so associated profiles, preferences, etc need to be sent with.
     * @return true if this node lives under an authorizable
     */
    boolean isAuthorizablePart() {
        try {
            JCRNodeDecorator parent = new JCRNodeDecorator(getParent())
            while(!parent.isAuthorizableType()) {
                parent = new JCRNodeDecorator(parent.getParent())
            }
            return true
        } catch(PathNotFoundException | ItemNotFoundException ex) {
            return false
        }
    }


    String getPrimaryType() {
        innerNode.getProperty(JCR_PRIMARYTYPE).string
    }


    boolean isAuthorizableType() {
        return primaryType == 'rep:User' || primaryType == 'rep:Group'
    }

    boolean isACType() {
        AC_NODETYPE_NAMES.contains(primaryType)
    }


    boolean isLoginToken() {
        final primaryType = getPrimaryType()
        return (primaryType == 'rep:Unstructured' && name.tokenize('/')[-1] == '.tokens') || primaryType == 'rep:Token'
    }


    Object asType(Class clazz) {
        if(clazz == JCRNode) {
            return innerNode
        }
        else {
            super.asType(clazz)
        }
    }

    @Override
    boolean equals(Object obj) {
        if (this.is(obj)) return true
        if (getClass() != obj.class) return false

        JCRNodeDecorator that = (JCRNodeDecorator)obj

        return this.hashCode() == that.hashCode()
    }

    @Override
    int hashCode() {
        return innerNode.getName().hashCode()
    }

    @CompileStatic
    private static class NoRootInclusionPolicy implements InclusionPolicy<JCRNode> {

        final JCRNodeDecorator rootNode

        NoRootInclusionPolicy(JCRNode rootNode) {
            this.rootNode = new JCRNodeDecorator(rootNode)
        }


        @Override
        boolean include(JCRNode node) {
            final JCRNodeDecorator candidateNode = new JCRNodeDecorator(node)
            //Don't include the root, and dont' include mandatory nodes as they are held within their parent
            return (!rootNode.equals(candidateNode)) && (!candidateNode.isMandatoryNode())
        }
    }
}
