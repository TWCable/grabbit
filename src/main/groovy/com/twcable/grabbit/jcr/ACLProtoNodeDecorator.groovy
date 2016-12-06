/*
 *
 *  * Copyright 2015 Time Warner Cable, Inc.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.twcable.grabbit.jcr

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import java.security.AccessControlException
import java.security.Principal
import javax.annotation.Nonnull
import javax.jcr.PathNotFoundException
import javax.jcr.Session
import javax.jcr.Value
import javax.jcr.security.AccessControlEntry
import javax.jcr.security.AccessControlManager
import javax.jcr.security.AccessControlPolicy
import javax.jcr.security.AccessControlPolicyIterator
import javax.jcr.security.Privilege
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList
import org.apache.jackrabbit.api.security.principal.PrincipalManager
import org.apache.sling.jcr.base.util.AccessControlUtil


import static com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode

/**
 * Wraps a rep:policy (rep:ACL) node, providing the ability to write it, and it's child ACE, and restriction nodes
 */
@CompileStatic
@Slf4j
class ACLProtoNodeDecorator extends ProtoNodeDecorator {


    protected ACLProtoNodeDecorator(@Nonnull ProtoNode repACLNode, @Nonnull Collection<ProtoPropertyDecorator> protoProperties, String nameOverride) {
        this.innerProtoNode = repACLNode
        this.protoProperties = protoProperties
        this.nameOverride = nameOverride
    }


    @Override
    protected JCRNodeDecorator writeNode(@Nonnull Session session) {
        /**
         * We don't write the rep:policy node directly. Rather, we find the rep:policy node's ACE(s) and add them to the
         * owner's existing policy; or we add them to a new policy.
         */
        clearExistingEntries(session)
        innerProtoNode.mandatoryChildNodeList.each { ProtoNode node ->
            if(isGrantACEType(node)) {
                writeGrantACE(session, node)
            }
            else if(isDenyACEType(node)) {
                writeDenyACE(session, node)
            }
        }
        session.save()
        try {
            return new JCRNodeDecorator(session.getNode(getName()))
        } catch(PathNotFoundException ex) {
            //We may not have been able to write the policy node if for example the principal does not exist
            return new JCRNodeDecorator(session.getNode(getParentPath()))
        }
    }


    private void clearExistingEntries(final Session session) {
        final JackrabbitAccessControlList acl = getAccessControlList(session)
        if(acl.accessControlEntries.length != 0) {
            acl.accessControlEntries.each { AccessControlEntry entry ->
                acl.removeAccessControlEntry(entry)
            }
            getAccessControlManager(session).setPolicy(getParentPath(), acl)
            session.save()
        }
    }


    private void writeGrantACE(final Session session, ProtoNode grantACENode) {
        writeACE(session, grantACENode, true)
    }


    private void writeDenyACE(final Session session, ProtoNode denyACENode) {
        writeACE(session, denyACENode, false)
    }


    private void writeACE(final Session session, ProtoNode aceNode, boolean grant) {
        JackrabbitAccessControlList acl = getAccessControlList(session)
        final String principalName = getPrincipalName(aceNode)
        Principal principal = getPrincipal(session, principalName)
        if(principal == null) {
            log.warn "Principal for name ${principalName} does not exist, or is not accessible. Can not write ACE/ACL information for this principal on ${getParentPath()}. If this principal is currently being synched, it may not be accessible."
            return
        }
        Privilege[] privileges = getPrivilegeNames(aceNode).collect { String privilegeName ->
            if(isSupportedPrivilege(session, privilegeName)) {
                return getPrivilege(session, privilegeName)
            }
            else {
                throw new IllegalStateException("${privilegeName} is not a supported privilege on this JCR implementation. Bailing out")
            }
        }
        Map<String, Value> restrictionMap
        if(hasRestrictions(aceNode)) {
            ProtoNode restrictionNode = getRestrictionNode(aceNode)
            restrictionMap = extractRestrictionMapFrom(restrictionNode)
        }
        if(restrictionMap != null) {
            acl.addEntry(principal, privileges, grant, restrictionMap)
        }
        else {
            acl.addEntry(principal, privileges, grant)
        }
        getAccessControlManager(session).setPolicy(getParentPath(), acl)
    }


    private boolean isGrantACEType(ProtoNode node) {
        return node.propertiesList.any { new ProtoPropertyDecorator(it).isGrantACEType() }
    }


    private boolean isDenyACEType(ProtoNode node) {
        return node.propertiesList.any { new ProtoPropertyDecorator(it).isDenyACEType() }
    }


    private boolean hasRestrictions(ProtoNode aceNode) {
        return aceNode.mandatoryChildNodeList.any { ProtoNode childNode ->
            childNode.propertiesList.any { new ProtoPropertyDecorator(it).isRepRestrictionType() }
        }
    }


    private ProtoNode getRestrictionNode(ProtoNode aceNode) {
        return aceNode.mandatoryChildNodeList.find { ProtoNode childNode ->
            childNode.propertiesList.any { new ProtoPropertyDecorator(it).isRepRestrictionType() }
        }
    }


    private String getPrincipalName(ProtoNode node) {
        node.propertiesList.collect { new ProtoPropertyDecorator(it) }.find { ProtoPropertyDecorator property ->
            property.isPrincipalName()
        }.getStringValue()
    }


    private String[] getPrivilegeNames(ProtoNode node) {
        final ProtoPropertyDecorator privilegeProperty = node.propertiesList.collect { new ProtoPropertyDecorator(it) }.find { ProtoPropertyDecorator property ->
            property.isPrivilege()
        }
        return privilegeProperty.getStringValues().toArray() as String[]
    }


    private Map<String, Value> extractRestrictionMapFrom(ProtoNode restrictionNode) {
        final Collection<ProtoPropertyDecorator> restrictionProperties = restrictionNode.propertiesList.findResults {
            final property = new ProtoPropertyDecorator(it)
            return !property.isPrimaryType() ? property : null
        }
        return restrictionProperties.collectEntries { ProtoPropertyDecorator property ->
            [(property.getName()) : property.getPropertyValue()]
        }
    }


    private JackrabbitAccessControlList getAccessControlList(final Session session) {
        final AccessControlManager manager = getAccessControlManager(session)
        final String ownershipNodePath = getParentPath()
        //Check to see if existing policies exist for this node
        final AccessControlPolicy[] existingPolicies = manager.getPolicies(ownershipNodePath)
        //If no policies, we need to create a new policy
        if(existingPolicies.length == 0) {
            final AccessControlPolicyIterator policyIterator = manager.getApplicablePolicies(ownershipNodePath)
            //For Jackrabbit, the only policy type we are interested in is the JackrabbitAccessControlList
            while (policyIterator.hasNext()) {
                final AccessControlPolicy thisPolicy = policyIterator.nextAccessControlPolicy()
                if (thisPolicy instanceof JackrabbitAccessControlList) {
                    return (JackrabbitAccessControlList)thisPolicy
                }
            }
        }
        //We have an existing policy. We just need to dig it out
        else {
            return existingPolicies.findResult { AccessControlPolicy policy ->
                if(policy instanceof JackrabbitAccessControlList) {
                    return (JackrabbitAccessControlList)policy
                }
            } as JackrabbitAccessControlList
        }
        throw new IllegalStateException("Something went wrong when trying to find a ${JackrabbitAccessControlList.class.canonicalName} for this session on node ${ownershipNodePath}")
    }


    private Principal getPrincipal(final Session session, final String principalName) {
        return getPrincipalManager(session).getPrincipal(principalName)
    }


    private Privilege getPrivilege(final Session session, final String privilegeName) {
        try {
            return getAccessControlManager(session).privilegeFromName(privilegeName)
        } catch(AccessControlException ex) {
            log.error "Something went wrong while getting privilege ${privilegeName}. isSupportedPrivilege() may not be producing expected behavior "
            throw ex
        }
    }


    private boolean isSupportedPrivilege(final Session session, final String privilegeName) {
        final Privilege[] supportedPrivileges = getAccessControlManager(session).getSupportedPrivileges(getParentPath())
        return supportedPrivileges.any { Privilege privilege -> privilege.getName() == privilegeName }
    }


    PrincipalManager getPrincipalManager(final Session session) {
        return AccessControlUtil.getPrincipalManager(session)
    }


    AccessControlManager getAccessControlManager(final Session session) {
        return AccessControlUtil.getAccessControlManager(session)
    }

}
