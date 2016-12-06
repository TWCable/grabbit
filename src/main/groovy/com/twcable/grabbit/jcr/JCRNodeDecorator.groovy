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
import javax.jcr.Session
import javax.jcr.Value as JcrValue
import javax.jcr.nodetype.ItemDefinition
import org.apache.jackrabbit.api.security.user.Authorizable
import org.apache.jackrabbit.api.security.user.UserManager
import org.apache.jackrabbit.commons.flat.TreeTraverser
import org.apache.jackrabbit.value.DateValue
import org.apache.sling.jcr.base.util.AccessControlUtil
import org.slf4j.Logger


import static org.apache.jackrabbit.JcrConstants.JCR_CREATED
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import static org.apache.jackrabbit.JcrConstants.MIX_REFERENCEABLE
import static org.apache.jackrabbit.commons.flat.TreeTraverser.ErrorHandler
import static org.apache.jackrabbit.commons.flat.TreeTraverser.InclusionPolicy
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.AC_NODETYPE_NAMES
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.NT_REP_ACL

@CompileStatic
@Slf4j
class JCRNodeDecorator {

    @Delegate
    JCRNode innerNode

    private final Collection<JCRPropertyDecorator> wrappedProperties

    //Evaluated in a lazy fashion
    private Collection<JCRNodeDecorator> immediateChildNodes
    private List<JCRNodeDecorator> childNodeList

    private final String transferredID


    JCRNodeDecorator(@Nonnull JCRNode node) {
        this(node, null)
    }


    JCRNodeDecorator(@Nonnull JCRNode node, @Nullable String transferredID) {
        if(!node) throw new IllegalArgumentException("node must not be null!")
        this.innerNode = node
        Collection<JcrProperty> innerProperties = node.properties.toList()
        this.wrappedProperties = innerProperties.collect { JcrProperty property ->
            new JCRPropertyDecorator(property, this)
        }
        this.transferredID = transferredID
    }


    private Collection<JCRPropertyDecorator> getWrappedProperties() { return wrappedProperties }


    /**
     * @return an identifier for this node on a sending server. Not all wrapped nodes will have this. Will return null if it does not exist
     */
    String getTransferredID() { return transferredID }


    /**
     * @return this node's immediate children (depth 1), empty if none
     */
    Collection<JCRNodeDecorator> getImmediateChildNodes() {
        if(!immediateChildNodes) {
            immediateChildNodes = (getNodes().collect { JCRNode node -> new JCRNodeDecorator(node) }  ?: []) as Collection<JCRNodeDecorator>
        }
        return immediateChildNodes
    }

    /**
     * @return this node's children via pre-order traversal.
     */
    List<JCRNodeDecorator> getChildNodeList() {
        if(!childNodeList) {
            childNodeList = (getChildNodeIterator().collect { JCRNode node -> new JCRNodeDecorator(node) } ?: []) as List<JCRNodeDecorator>
        }
        return childNodeList
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
    * Identify all required child nodes. This may include any mandatory nodes per definition, associated nodes that are required to write this node info on another server,
    * and nodes that are referenced via this node's weak/strong reference pointers
    * @return list of immediate required child nodes that must be transported with this node, or an empty collection if no required nodes
    */
    @Nullable
    Collection<JCRNodeDecorator> getRequiredChildNodes() {
        final Collection<JCRNodeDecorator> requiredNodes = []
        if(isAuthorizableType()){
            requiredNodes.addAll(getChildNodeList().findAll { JCRNodeDecorator childJcrNode -> !childJcrNode.isLoginToken() && !childJcrNode.isACType() })
        }
        else if(isRepACLType()) {
            //Send all ACE parts underneath the ACL as required nodes
            requiredNodes.addAll(getChildNodeList())
        }
        else {
            requiredNodes.addAll(getMandatoryChildren())
        }
        requiredNodes.addAll(referencedNodesFrom(requiredNodes + this))
        return requiredNodes
    }


    String getPrimaryType() {
        innerNode.getProperty(JCR_PRIMARYTYPE).string
    }


    /**
     * Find any weak or strong references on this collection of nodes, and attempt to find their nodes
     * @return any nodes we can find through references
     */
    private Collection<JCRNodeDecorator> referencedNodesFrom(Collection<JCRNodeDecorator> nodes) {
        final Collection<JCRPropertyDecorator> referenceProperties = nodes.collectMany { JCRNodeDecorator node ->
            /**
             * Find all references that are transferable. We don't want to send referenced nodes that belong to a protected property that are not being transferred back to the client,
             * such as version storage entries
             */
            return node.getWrappedProperties().findAll { JCRPropertyDecorator property -> property.isReferenceType() && property.isTransferable() }
        }
        return referenceProperties.collectMany { JCRPropertyDecorator property ->
            property.values.toList().findResults { JcrValue value ->
                try {
                    return new JCRNodeDecorator(getSession().getNodeByIdentifier(value.string))
                } catch(ItemNotFoundException ex) {
                    _log().info "Tried following reference ${value.string} from ${getInnerNode().path}, but this node does not exist any longer. Skipping"
                }
            }
        } as Collection<JCRNodeDecorator>
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


    @Nonnull
     ProtoNode toProtoNode() {
        final ProtoNodeBuilder protoNodeBuilder = ProtoNode.newBuilder()
        protoNodeBuilder.setName(path)
        if(isNodeType(MIX_REFERENCEABLE)) {
            protoNodeBuilder.setIdentifier(getIdentifier())
        }
        protoNodeBuilder.addAllProperties(getProtoProperties())
        requiredChildNodes.each {
            protoNodeBuilder.addMandatoryChildNode(it.toProtoNode())
        }
        return protoNodeBuilder.build()
    }


    @Nonnull
    private Collection<ProtoProperty> getProtoProperties() {
        final Collection<JCRPropertyDecorator> transferableProperties = getWrappedProperties().findAll{ it.isTransferable() }
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

    /**
     * @return is part of a rep:ACL tree, such as rep:GrantACE, or rep:restrictions. These nodes are transported with the rep:ACL parent, and
     * are not written independently.
     */
    boolean isACPart() {
        final Collection<String> theACNodeTypeNames = new ArrayList(AC_NODETYPE_NAMES)
        theACNodeTypeNames.remove(NT_REP_ACL)
        return theACNodeTypeNames.contains(primaryType)
    }


    boolean isRepACLType() {
        return primaryType == NT_REP_ACL
    }


    boolean isACType() {
        return AC_NODETYPE_NAMES.contains(primaryType)
    }


    boolean isAuthorizableType() {
        return primaryType == 'rep:User' || primaryType == 'rep:Group'
    }


    boolean isLoginToken() {
        final primaryType = getPrimaryType()
        return (primaryType == 'rep:Unstructured' && name.tokenize('/')[-1] == '.tokens') || primaryType == 'rep:Token'
    }


    boolean isReferenceable() {
        return isNodeType(MIX_REFERENCEABLE)
    }


    /**
     * For ease of mocking. Simply delegates to static accessor in AccessControlUtil
     */
    UserManager getUserManager(final Session session) {
        return AccessControlUtil.getUserManager(session)
    }


    /**
     * For ease of mocking
     */
    Iterator<JCRNode> getChildNodeIterator() {
        return TreeTraverser.nodeIterator(innerNode, ErrorHandler.IGNORE, new NoRootInclusionPolicy(this))
    }


    Object asType(Class clazz) {
        if(clazz == JCRNode) {
            return innerNode
        }
        else if(clazz == Authorizable) {
            if(!isAuthorizableType()) throw new ClassCastException('This class is not an Authorizable type. Check isAuthorizableType() before casting.')
            return getUserManager(session).getAuthorizableByPath(getPath())
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

    /**
     * @SL4J generated log property, and @Delegate conflict on accessing log sometimes within closures. This is to get around that
     */
    private static Logger _log() {
        return log
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
