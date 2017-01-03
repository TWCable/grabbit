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
import com.twcable.grabbit.proto.NodeProtos.Value as ProtoValue
import com.twcable.grabbit.security.AuthorizablePrincipal
import com.twcable.grabbit.security.InsufficientGrabbitPrivilegeException
import java.lang.reflect.ReflectPermission
import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.PropertyIterator
import javax.jcr.Session
import javax.jcr.nodetype.NodeType
import org.apache.jackrabbit.api.security.user.Authorizable
import org.apache.jackrabbit.api.security.user.Group
import org.apache.jackrabbit.api.security.user.User
import org.apache.jackrabbit.api.security.user.UserManager
import org.apache.jackrabbit.value.StringValue
import spock.lang.Specification
import spock.lang.Unroll


import static javax.jcr.PropertyType.STRING
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE


class AuthorizableProtoNodeDecoratorSpec extends Specification {

    AuthorizableProtoNodeDecorator theProtoNodeDecorator(boolean forUser, boolean hasProfile, boolean hasPreferences, Closure configuration = null){
        ProtoNodeBuilder nodeBuilder = ProtoNode.newBuilder()
        nodeBuilder.setName(forUser ? "/home/users/u/user1" : "/home/groups/g/group1")

        ProtoProperty primaryTypeProperty = ProtoProperty
                .newBuilder()
                .setName(JCR_PRIMARYTYPE)
                .setType(STRING)
                .setMultiple(false)
                .addValues(ProtoValue.newBuilder().setStringValue(forUser ? 'rep:User' : 'rep:Group'))
                .build()
        nodeBuilder.addProperties(primaryTypeProperty)

        ProtoProperty disabledProperty =  ProtoProperty
                .newBuilder()
                .setName('rep:disabled')
                .setType(STRING)
                .setMultiple(false)
                .addValues(ProtoValue.newBuilder().setStringValue('Reason for disabling'))
                .build()
        nodeBuilder.addProperties(disabledProperty)

        ProtoProperty authorizableIdProperty = ProtoProperty
                .newBuilder()
                .setName('rep:authorizableId')
                .setType(STRING)
                .setMultiple(false)
                .addValues(ProtoValue.newBuilder().setStringValue('authorizableID'))
                .build()
        nodeBuilder.addProperties(authorizableIdProperty)

        ProtoProperty authorizableCategory = ProtoProperty
                .newBuilder()
                .setName('cq:authorizableCategory')
                .setType(STRING)
                .setMultiple(false)
                .addValues(ProtoValue.newBuilder().setStringValue('mcm'))
                .build()
        nodeBuilder.addProperties(authorizableCategory)

        ProtoProperty simplePrimaryType = ProtoProperty
                .newBuilder()
                .setName(JCR_PRIMARYTYPE)
                .setType(STRING)
                .setMultiple(false)
                .addValues(ProtoValue.newBuilder().setStringValue('nt:unstructured'))
                .build()
        nodeBuilder.addProperties(authorizableCategory)
        if(hasPreferences) {
            ProtoNode preferenceNode = ProtoNode.newBuilder()
                .setName("${nodeBuilder.getName()}/preferences")
                .addProperties(simplePrimaryType)
                .build()
            nodeBuilder.addMandatoryChildNode(preferenceNode)
        }
        if(hasProfile) {
            ProtoNode profileNode = ProtoNode
                .newBuilder()
                .setName("${nodeBuilder.getName()}/profile")
                .addProperties(simplePrimaryType)
                .build()
            nodeBuilder.addMandatoryChildNode(profileNode)
        }
        final properties = [new ProtoPropertyDecorator(primaryTypeProperty), new ProtoPropertyDecorator(disabledProperty), new ProtoPropertyDecorator(authorizableIdProperty), new ProtoPropertyDecorator(authorizableCategory)]
        return GroovySpy(AuthorizableProtoNodeDecorator, constructorArgs: [nodeBuilder.build(), properties], configuration)
    }


    def "Throws an InsufficientGrabbitPrivilegeException if JVM permissions are not present"() {
        when:
        final protoNodeDecorator = theProtoNodeDecorator(false, false, false) {
            it.getSecurityManager() >> Mock(SecurityManager) {
                it.checkPermission(permission) >> {
                    throw new SecurityException()
                }
            }
        }

        protoNodeDecorator.writeToJcr(Mock(Session))

        then:
        thrown(InsufficientGrabbitPrivilegeException)

        where:
        permission << [new ReflectPermission('suppressAccessChecks'), new RuntimePermission('accessDeclaredMembers'), new RuntimePermission('accessClassInPackage.{org.apache.jackrabbit.oak.security.user}')]
    }


    def "Passes security check if all JVM permissions are present"() {
        when:
        final session = Mock(Session) {
            it.getNode('authorizablePath') >> Mock(Node) {
                getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                    it.getString() >> 'nt:unstructured'
                }
                getProperties() >> Mock(PropertyIterator) {
                    it.toList() >> []
                }
                it.getPrimaryNodeType() >> Mock(NodeType) {
                    it.canSetProperty(_, _) >> false
                }
            }
        }
        final protoNodeDecorator = theProtoNodeDecorator(false, false, false) {
            it.getSecurityManager() >> Mock(SecurityManager) {
                it.checkPermission(permission) >> {
                    return
                }
            }
            it.getUserManager(session) >> Mock(UserManager) {
                it.createGroup(_, _, _) >> Mock(Group) {
                    it.getPath() >> 'authorizablePath'
                }
            }
        }

        protoNodeDecorator.writeToJcr(session)

        then:
        notThrown(InsufficientGrabbitPrivilegeException)

        where:
        permission << [new ReflectPermission('suppressAccessChecks'), new RuntimePermission('accessDeclaredMembers'), new RuntimePermission('accessClassInPackage.{org.apache.jackrabbit.oak.security.user}')]
    }


    def "Passes security check if no SecurityManager is found on the JVM"() {
        when:
        final session = Mock(Session) {
            it.getNode('authorizablePath') >> Mock(Node) {
                getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                    it.getString() >> 'nt:unstructured'
                }
                getProperties() >> Mock(PropertyIterator) {
                    it.toList() >> []
                }
                it.getPrimaryNodeType() >> Mock(NodeType) {
                    it.canSetProperty(_, _) >> false
                }
            }
        }
        final protoNodeDecorator = theProtoNodeDecorator(false, false, false) {
            it.getSecurityManager() >> null
            it.getUserManager(session) >> Mock(UserManager) {
                it.createGroup(_, _, _) >> Mock(Group) {
                    it.getPath() >> 'authorizablePath'
                }
            }
        }

        protoNodeDecorator.writeToJcr(session)

        then:
        notThrown(InsufficientGrabbitPrivilegeException)
    }


    def "getSecurityManager() will retrieve the JVM's security manager"() {
        given:
        final protoNodeDecorator = theProtoNodeDecorator(false, false, false)

        expect:
        System.getSecurityManager() == protoNodeDecorator.getSecurityManager()
    }


    def "writeToJcr() will create a new underlying User, and return it's node within a JcrNodeDecorator"(){
        given:
        final node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getPrimaryNodeType() >> Mock(NodeType) {
                it.canSetProperty(_, _) >> false
            }
        }
        final session = Mock(Session) {
            it.getNode('newUserPath') >> node
        }
        final newUser = Mock(User) {
            1 * it.disable('Reason for disabling')
            1 * it.setProperty('cq:authorizableCategory', new StringValue('mcm'))
            it.getPath() >> 'newUserPath'
        }
        final protoNodeDecorator = theProtoNodeDecorator(true, false, false) {
            it.getName() >> '/home/users/auth_folder/user'
            it.getSecurityManager() >> null
            it.setPasswordForUser(newUser, session) >> {
                return
            }
            it.getUserManager(session) >> Mock(UserManager) {
                it.getAuthorizable('authorizableID') >> null
                1 * it.createUser('authorizableID', _, new AuthorizablePrincipal('authorizableID'), '/home/users/auth_folder') >> newUser
            }
        }

        when:
        final userNode = protoNodeDecorator.writeToJcr(session)

        then:
        userNode.getInnerNode() ==  node
    }


    def "writeToJcr() will create a new underlying Group, and return it's node within a JcrNodeDecorator"(){
        given:
        final node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getPrimaryNodeType() >> Mock(NodeType) {
                it.canSetProperty(_, _) >> false
            }
        }
        final session = Mock(Session) {
            it.getNode('newGroupPath') >> node
        }
        final newGroup = Mock(Group) {
            it.getPath() >> 'newGroupPath'
        }
        final protoNodeDecorator = theProtoNodeDecorator(false, false, false) {
            it.getName() >> '/home/groups/auth_folder/group'
            it.getSecurityManager() >> null
            it.getUserManager(session) >> Mock(UserManager) {
                it.getAuthorizable('authorizableID') >> null
                1 * it.createGroup('authorizableID', new AuthorizablePrincipal('authorizableID'), '/home/groups/auth_folder') >> newGroup
            }
        }

        when:
        final groupNode = protoNodeDecorator.writeToJcr(session)

        then:
        groupNode.getInnerNode() ==  node
    }

    @Unroll
    def "Updates profile on an authorizable if it exists. Exists: #exists"() {
        when:
        final node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> 'rep:User'
            }
            getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getPrimaryNodeType() >> Mock(NodeType) {
                it.canSetProperty(_, _) >> false
            }
        }
        final session = Mock(Session) {
            it.getNode('/home/users/u/newuser') >> node
        }
        final protoNodeDecorator = theProtoNodeDecorator(false, exists, false) {
            it.getSecurityManager() >> null
            it.getUserManager(session) >> Mock(UserManager) {
                it.getAuthorizable('authorizableID') >> Mock(Authorizable)
                it.createGroup('authorizableID', _, _) >> Mock(Group) {
                    it.getPath() >> '/home/users/u/newuser'
                }
            }
            (exists ? 1 : 0) * it.createFrom(_ as ProtoNode, '/home/users/u/newuser/profile')  >> Mock(ProtoNodeDecorator)
        }

        then:
        protoNodeDecorator.writeToJcr(session)


        where:
        exists << [false, true]
    }

    @Unroll
    def "Updates preferences on an authorizable if it exists. Exists: #exists"() {
        when:
        final node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                it.getString() >> 'rep:User'
            }
            getProperties() >> Mock(PropertyIterator) {
                it.toList() >> []
            }
            it.getPrimaryNodeType() >> Mock(NodeType) {
                it.canSetProperty(_, _) >> false
            }
        }
        final session = Mock(Session) {
            it.getNode('/home/users/u/newuser') >> node
        }
        final protoNodeDecorator = theProtoNodeDecorator(false, false, exists) {
            it.getSecurityManager() >> null
            it.getUserManager(session) >> Mock(UserManager) {
                it.getAuthorizable('authorizableID') >> Mock(Authorizable)
                it.createGroup('authorizableID', _, _) >> Mock(Group) {
                    it.getPath() >> '/home/users/u/newuser'
                }
            }
            (exists ? 1 : 0) * it.createFrom(_ as ProtoNode, '/home/users/u/newuser/preferences')  >> Mock(ProtoNodeDecorator)
        }

        then:
        protoNodeDecorator.writeToJcr(session)


        where:
        exists << [false, true]
    }
}
