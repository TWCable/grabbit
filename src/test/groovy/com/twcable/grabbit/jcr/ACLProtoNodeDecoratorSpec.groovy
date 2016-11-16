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

import com.twcable.grabbit.proto.NodeProtos.Node as ProtoNode
import com.twcable.grabbit.proto.NodeProtos.Node.Builder as NodeBuilder
import com.twcable.grabbit.proto.NodeProtos.Property as ProtoProperty
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import java.security.AccessControlException
import java.security.Principal
import javax.jcr.Node
import javax.jcr.PathNotFoundException
import javax.jcr.Property
import javax.jcr.PropertyIterator
import javax.jcr.Session
import javax.jcr.security.AccessControlEntry
import javax.jcr.security.AccessControlManager
import javax.jcr.security.AccessControlPolicy
import javax.jcr.security.AccessControlPolicyIterator
import javax.jcr.security.NamedAccessControlPolicy
import javax.jcr.security.Privilege
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList
import org.apache.jackrabbit.api.security.principal.PrincipalManager
import spock.lang.Specification


import static javax.jcr.PropertyType.NAME
import static javax.jcr.PropertyType.STRING
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.NT_REP_ACL
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.NT_REP_DENY_ACE
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.NT_REP_GRANT_ACE
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.NT_REP_RESTRICTIONS
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.REP_PRINCIPAL_NAME
import static org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants.REP_PRIVILEGES

class ACLProtoNodeDecoratorSpec extends Specification {

    ACLProtoNodeDecorator theProtoNodeDecorator(Closure configuration = null) {
        NodeBuilder repPolicyNodeBuilder = ProtoNode.newBuilder()
        repPolicyNodeBuilder.setName('/content/test/rep:policy')
        repPolicyNodeBuilder.addProperties(
            ProtoProperty.newBuilder()
                .setName(JCR_PRIMARYTYPE)
                .setType(STRING)
                .setMultiple(false)
                .addValues(ProtoValue.newBuilder().setStringValue(NT_REP_ACL))
        )

        repPolicyNodeBuilder.addMandatoryChildNode(
            ProtoNode.newBuilder()
                .setName('/content/test/rep:policy/allow')
                .addAllProperties([
                    ProtoProperty.newBuilder()
                        .setName(JCR_PRIMARYTYPE)
                        .setType(STRING)
                        .setMultiple(false)
                        .addValues(ProtoValue.newBuilder().setStringValue(NT_REP_GRANT_ACE)).build(),
                    ProtoProperty.newBuilder()
                        .setName(REP_PRINCIPAL_NAME)
                        .setType(STRING)
                        .setMultiple(false)
                        .addValues(ProtoValue.newBuilder().setStringValue('test-principal')).build(),
                    ProtoProperty.newBuilder()
                        .setName(REP_PRIVILEGES)
                        .setType(NAME)
                        .setMultiple(true)
                        .addAllValues([
                            ProtoValue.newBuilder().setStringValue('jcr:read').build(),
                            ProtoValue.newBuilder().setStringValue('jcr:write').build()
                        ]).build()
                ])
        )
        repPolicyNodeBuilder.addMandatoryChildNode(
            ProtoNode.newBuilder()
                .setName('/content/test/rep:policy/deny')
                .addAllProperties([
                    ProtoProperty.newBuilder()
                        .setName(JCR_PRIMARYTYPE)
                        .setType(STRING)
                        .setMultiple(false)
                        .addValues(ProtoValue.newBuilder().setStringValue(NT_REP_DENY_ACE)).build(),
                    ProtoProperty.newBuilder()
                        .setName(REP_PRINCIPAL_NAME)
                        .setType(STRING)
                        .setMultiple(false)
                        .addValues(ProtoValue.newBuilder().setStringValue('test-principal2')).build(),
                    ProtoProperty.newBuilder()
                        .setName(REP_PRIVILEGES)
                        .setType(NAME)
                        .setMultiple(false)
                        .addValues(ProtoValue.newBuilder().setStringValue('jcr:write')).build()
                ])
                .addMandatoryChildNode(
                    ProtoNode.newBuilder()
                        .setName('/content/test/rep:policy/deny/rep:restrictions')
                        .addAllProperties([
                            ProtoProperty.newBuilder()
                                .setName(JCR_PRIMARYTYPE)
                                .setType(STRING)
                                .setMultiple(false)
                                .addValues(ProtoValue.newBuilder().setStringValue(NT_REP_RESTRICTIONS)).build(),
                            ProtoProperty.newBuilder()
                                .setName('rep:ntNames')
                                .setType(NAME)
                                .setMultiple(false)
                                .addValues(ProtoValue.newBuilder().setStringValue('cq:meta')).build()
                        ])
                )
        )

        return GroovySpy(ACLProtoNodeDecorator, constructorArgs: [repPolicyNodeBuilder.build(), [] as Collection<ProtoPropertyDecorator>, null], configuration)
    }

    def "writeToJcr() with new policies"() {
        when:
        final String ownerNodePath = '/content/test'
        final Node ownerNode = Mock(Node) {
            it.getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> 'nt:unstructured'
            }
            it.getName() >> ownerNodePath
        }
        final Node repPolicyNode = Mock(Node) {
            it.getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> NT_REP_ACL
            }
            it.getName() >> "${ownerNodePath}/rep:policy"
        }
        final Session session = Mock(Session) {
            1 * it.save()
            it.getNode(ownerNodePath) >> ownerNode
            it.getNode("${ownerNodePath}/rep:policy") >> repPolicyNode
        }
        final Principal principalOne = Mock(Principal)
        final Principal principalTwo = Mock(Principal)
        final JackrabbitAccessControlList aclList = Mock(JackrabbitAccessControlList) {
            //Principal two receives a deny entry with restrictions
            1 * it.addEntry(principalTwo, _, false, !null)
            //Principal one receives an approve entry with no restrictions
            1 * it.addEntry(principalOne, _, true)
            it.getAccessControlEntries() >> ([].toArray() as AccessControlEntry[])
        }

        final ACLProtoNodeDecorator aclProtoNodeDecorator = theProtoNodeDecorator {
            it.getAccessControlManager(session) >> Mock(AccessControlManager) {
                it.getPolicies(ownerNodePath) >> ([].toArray() as AccessControlPolicy[])
                it.getSupportedPrivileges(ownerNodePath) >> [
                    Mock(Privilege) {
                        it.getName() >> 'jcr:read'
                    },
                    Mock(Privilege) {
                        it.getName() >> 'jcr:write'
                    },
                    Mock(Privilege) {
                        it.getName() >> 'crx:replicate'
                    }
                ]
                it.privilegeFromName('jcr:read') >> Mock(Privilege)
                it.privilegeFromName('jcr:write') >> Mock(Privilege)
                it.privilegeFromName({ it != 'jcr:read' && it != 'jcr:write'}) >> { throw new AccessControlException() }
                //Two policies to set
                2 * it.setPolicy(ownerNodePath, _)
                it.getApplicablePolicies(ownerNodePath) >> Mock(AccessControlPolicyIterator) {
                    it.hasNext() >>> true >> true >> true >> true >> true >> true >> false
                    it.nextAccessControlPolicy() >>> Mock(NamedAccessControlPolicy) >> aclList >> Mock(NamedAccessControlPolicy) >> aclList >> Mock(NamedAccessControlPolicy) >> aclList
                }
            }
            it.getPrincipalManager(session) >> Mock(PrincipalManager) {
                it.getPrincipal('test-principal') >> principalOne
                it.getPrincipal('test-principal2') >> principalTwo
            }
        }

        final JCRNodeDecorator result = aclProtoNodeDecorator.writeToJcr(session)

        then:
        result.getName() == "${ownerNodePath}/rep:policy".toString()
    }


    def "writeToJcr() with existing policies"() {
        when:
        final String ownerNodePath = '/content/test'
        final Node ownerNode = Mock(Node) {
            it.getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> 'nt:unstructured'
            }
            it.getName() >> ownerNodePath
        }
        final Node repPolicyNode = Mock(Node) {
            it.getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> NT_REP_ACL
            }
            it.getName() >> "${ownerNodePath}/rep:policy"
        }
        final Session session = Mock(Session) {
            2 * it.save()
            it.getNode(ownerNodePath) >> ownerNode
            it.getNode("${ownerNodePath}/rep:policy") >> repPolicyNode
        }
        final Principal principalOne = Mock(Principal)
        final Principal principalTwo = Mock(Principal)
        final JackrabbitAccessControlList aclList = Mock(JackrabbitAccessControlList) {
            //Principal two receives a deny entry with restrictions
            1 * it.addEntry(principalTwo, _, false, !null)
            //Principal one receives an approve entry with no restrictions
            1 * it.addEntry(principalOne, _, true)
            final AccessControlEntry entry = Mock(AccessControlEntry)
            it.getAccessControlEntries() >> [entry].toArray()
            1 * it.removeAccessControlEntry(entry)
        }

        final ACLProtoNodeDecorator aclProtoNodeDecorator = theProtoNodeDecorator {
            it.getAccessControlManager(session) >> Mock(AccessControlManager) {
                it.getPolicies(ownerNodePath) >> [
                    Mock(NamedAccessControlPolicy),
                    aclList
                ]
                it.getSupportedPrivileges(ownerNodePath) >> [
                        Mock(Privilege) {
                            it.getName() >> 'jcr:read'
                        },
                        Mock(Privilege) {
                            it.getName() >> 'jcr:write'
                        },
                        Mock(Privilege) {
                            it.getName() >> 'crx:replicate'
                        }
                ]
                it.privilegeFromName('jcr:read') >> Mock(Privilege)
                it.privilegeFromName('jcr:write') >> Mock(Privilege)
                it.privilegeFromName({ it != 'jcr:read' && it != 'jcr:write'}) >> { throw new AccessControlException() }
                //Two policies to set + updating policy to remove existing entry
                3 * it.setPolicy(ownerNodePath, _)
            }
            it.getPrincipalManager(session) >> Mock(PrincipalManager) {
                it.getPrincipal('test-principal') >> principalOne
                it.getPrincipal('test-principal2') >> principalTwo
            }
        }

        final JCRNodeDecorator result = aclProtoNodeDecorator.writeToJcr(session)

        then:
        result.getName() == "${ownerNodePath}/rep:policy".toString()
    }


    def "throws IllegalStateException when attempting to write an unsupported privilege"() {
        when:
        final String ownerNodePath = '/content/test'
        final Node ownerNode = Mock(Node) {
            it.getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> 'nt:unstructured'
            }
            it.getName() >> ownerNodePath
        }
        final Node repPolicyNode = Mock(Node) {
            it.getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> NT_REP_ACL
            }
            it.getName() >> "${ownerNodePath}/rep:policy"
        }
        final Session session = Mock(Session) {
            it.getNode(ownerNodePath) >> ownerNode
            it.getNode("${ownerNodePath}/rep:policy") >> repPolicyNode
        }
        final Principal principalOne = Mock(Principal)
        final Principal principalTwo = Mock(Principal)

        final ACLProtoNodeDecorator aclProtoNodeDecorator = theProtoNodeDecorator {
            it.getAccessControlManager(session) >> Mock(AccessControlManager) {
                it.getPolicies(ownerNodePath) >> [
                        Mock(NamedAccessControlPolicy),
                        Mock(JackrabbitAccessControlList) {
                            it.getAccessControlEntries() >> ([].toArray() as AccessControlEntry[])
                        }
                ]
                it.privilegeFromName(_) >> { throw new AccessControlException() }
            }
            it.getPrincipalManager(session) >> Mock(PrincipalManager) {
                it.getPrincipal('test-principal') >> principalOne
                it.getPrincipal('test-principal2') >> principalTwo
            }
        }

        aclProtoNodeDecorator.writeToJcr(session)

        then:
        thrown(IllegalStateException)
    }


    def "On writeToJcr() when attempting to write ACE information for non-existent principal, recover, and return parent node"() {
        when:
        final String ownerNodePath = '/content/test'
        final Node ownerNode = Mock(Node) {
            it.getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> 'nt:unstructured'
            }
            it.getName() >> ownerNodePath
        }
        final Session session = Mock(Session) {
            it.getNode(ownerNodePath) >> ownerNode
            it.getNode("${ownerNodePath}/rep:policy") >> { throw new PathNotFoundException() }
        }

        final ACLProtoNodeDecorator aclProtoNodeDecorator = theProtoNodeDecorator {
            it.getAccessControlManager(session) >> Mock(AccessControlManager) {
                it.getPolicies(ownerNodePath) >> [
                        Mock(JackrabbitAccessControlList) {
                            it.getAccessControlEntries() >> ([].toArray() as AccessControlEntry[])
                        }
                ]
            }
            it.getPrincipalManager(session) >> Mock(PrincipalManager)
        }

        final JCRNodeDecorator returnedNode = aclProtoNodeDecorator.writeToJcr(session)

        then:
        (returnedNode as Node) == ownerNode
    }
}
