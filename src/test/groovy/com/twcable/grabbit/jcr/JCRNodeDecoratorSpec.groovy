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
import javax.jcr.Binary
import javax.jcr.ItemNotFoundException
import javax.jcr.Node
import javax.jcr.NodeIterator
import javax.jcr.Property
import javax.jcr.PropertyIterator
import javax.jcr.RepositoryException
import javax.jcr.Value
import javax.jcr.nodetype.ItemDefinition
import javax.jcr.nodetype.NodeDefinition
import javax.jcr.nodetype.NodeType
import javax.jcr.nodetype.PropertyDefinition
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll


import static com.twcable.grabbit.jcr.JCRNodeDecorator.NoRootInclusionPolicy
import static com.twcable.grabbit.testutil.StubInputStream.inputStream
import static javax.jcr.PropertyType.BINARY
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@SuppressWarnings("GroovyAssignabilityCheck")
class JCRNodeDecoratorSpec extends Specification {
    @Shared
    static Calendar jcrModifiedDate = Calendar.getInstance()
    static Calendar cqModifiedDate = Calendar.getInstance()
    static Calendar jcrCreatedDate = Calendar.getInstance()

    def setupSpec(){
        jcrModifiedDate.setTime(new Date(2016,6,4))
        cqModifiedDate.setTime(new Date(2016,6,3))
        jcrCreatedDate.setTime(new Date(2016,6,2))
    }

    def "null nodes are not allowed for JCRNodeDecorator construction"() {
        when:
        new JCRNodeDecorator(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "toProtoNode()"() {
        given:
        Node node = Mock(Node) {
            getPath() >> "/some/path"
            getPrimaryNodeType() >> Mock(NodeType) {
                getChildNodeDefinitions() >> [
                    Mock(NodeDefinition) {
                        isMandatory() >> true
                    }
                ].toArray()
            }
            getNodes() >> Mock(NodeIterator) {
                hasNext() >>> true >> false
                next() >>
                    Mock(Node) {
                        getDefinition() >> Mock(NodeDefinition) {
                            isMandatory() >> true
                        }
                        getProperties() >> Mock(PropertyIterator) {
                            toList() >> []
                        }
                        getPath() >> "path"
                        getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                            getString() >> "nt:unstructured"
                        }
                        getPrimaryNodeType() >> Mock(NodeType) {
                            getChildNodeDefinitions() >> [].toArray()
                        }
                    }
            }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> "rep:SystemUser"
            }
            getProperties() >> new PropertyIteratorAdapter(
                [
                    Mock(Property) {
                        getName() >> JCR_PRIMARYTYPE
                        getString() >> "rep:SystemUser"
                        getDefinition() >> Mock(PropertyDefinition) {
                            isProtected() >> true
                        }
                        getType() >> BINARY
                        isMultiple() >> false
                        getValue() >> Mock(Value) {
                            getBinary() >> Mock(Binary) {
                                getStream() >> inputStream("test data")
                            }
                        }
                    },
                    Mock(Property) {
                        getName() >> JCR_LASTMODIFIED
                        getString() >> "lastModified"
                        getDefinition() >> Mock(PropertyDefinition) {
                            isProtected() >> false
                        }
                        getType() >> BINARY
                        isMultiple() >> false
                        getValue() >> Mock(Value) {
                            getBinary() >> Mock(Binary) {
                                getStream() >> inputStream("test data")
                            }                            }
                    },
                    Mock(Property) {
                        getName() >> "protectedProperty"
                        getString() >> "protectedPropertyValue"
                        getDefinition() >> Mock(PropertyDefinition) {
                            isProtected() >> true
                        }
                        getType() >> BINARY
                        isMultiple() >> false
                        getValue() >> Mock(Value) {
                            getBinary() >> Mock(Binary) {
                                getStream() >> inputStream("test data")
                            }
                        }
                    }
                ].iterator()
            )
        }

        when:
        final ProtoNode protoNode =  new JCRNodeDecorator(node).toProtoNode()

        then:
        protoNode.getName() == '/some/path'
        protoNode.getPropertiesCount() == 1
    }


    def "setLastModified() when last modified can be set"() {
        given:
        Node node = Mock(Node) {
            getPrimaryNodeType() >> Mock(NodeType) {
                canSetProperty(JCR_LASTMODIFIED, _) >> { true }
            }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)
        nodeDecorator.setLastModified()

        then:
        1 * node.setProperty(JCR_LASTMODIFIED, _)
        notThrown(RepositoryException)
    }


    def "setLastModified() when last modified can not be set"() {
        given:
        Node node = Mock(Node) {
            getPrimaryNodeType() >> Mock(NodeType) {
                canSetProperty(JCR_LASTMODIFIED, _) >> { false }
            }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)
        nodeDecorator.setLastModified()

        then:
        0 * node.setProperty(JCR_LASTMODIFIED, _)
        notThrown(RepositoryException)
    }


    def "During setLastModified() when something goes wrong with getPrimaryNodeType() we handle this case gracefully"() {
        given:
        Node node = Mock(Node) {
            getPrimaryNodeType() >> { throw new RepositoryException() }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)
        nodeDecorator.setLastModified()

        then:
        notThrown(RepositoryException)
    }


    def "isMandatoryNode()"() {
        given:
        Node node = Mock(Node) {
            getDefinition() >> Mock(NodeDefinition) {
                isMandatory() >> isMandatory
            }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)

        then:
        nodeDecorator.isMandatoryNode() == isMandatory

        where:
        isMandatory << [true, false]
    }


    def "hasMandatoryChildNodes()"() {
        given:
        Node node = Mock(Node) {
            getPrimaryNodeType() >> Mock(NodeType) {
                getChildNodeDefinitions() >> [
                        Mock(ItemDefinition) { isMandatory() >> firstDefinition } as NodeDefinition,
                        Mock(ItemDefinition) { isMandatory() >> secondDefinition } as NodeDefinition
                ]
            }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)

        then:
        nodeDecorator.hasMandatoryChildNodes() == hasMandatoryChildNodes

        where:
        hasMandatoryChildNodes  |   firstDefinition |   secondDefinition
        true                    |   true            |   false
        true                    |   true            |   true
        false                   |   false           |   false
    }


    def "getRequiredChildNodes()"() {
        given:
        Node nodeWithMandatoryChildren = Mock(Node) {
            getNodes() >> Mock(NodeIterator) {
                hasNext() >>> true >> false
                next() >> Mock(Node) {
                    getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                        getString() >> 'nt:resource'
                    }
                    getProperties() >> Mock(PropertyIterator) {
                        toList() >> []
                    }
                    getDefinition() >> Mock(NodeDefinition) {
                        isMandatory() >> true
                    }
                }
            }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:file'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        Node authorizableNode = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'rep:User'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when: "The node has mandatory children"
        final nodeDecorator = Spy(JCRNodeDecorator, constructorArgs: [nodeWithMandatoryChildren]) {
            hasMandatoryChildNodes() >> true
            isAuthorizableType() >> false
        }

        then:
        nodeDecorator.getRequiredChildNodes().size() == 1

        and: "The node has authorizable pieces"

        when:
        final otherNodeDecorator = Spy(JCRNodeDecorator, constructorArgs: [authorizableNode]) {
            hasMandatoryChildNodes() >> false
            isAuthorizableType() >> true
            getChildNodeIterator() >> Mock(Iterator) {
                hasNext() >>> true >> true >> true >> true >> false
                next() >>>
                    Mock(Node) {
                        getName() >> "/home/users/u/user/preferences"
                        getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                            getString() >> 'nt:unstructured'
                        }
                        getProperty('sling:resourceType') >> Mock(Property) {
                            getString() >> 'cq:Preferences'
                        }
                        getProperties() >> Mock(PropertyIterator) {
                            toList() >> []
                        }
                    } >>
                    Mock(Node) {
                        getName() >> "/home/users/u/user/profile"
                        getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                            getString() >> 'nt:unstructured'
                        }
                        getProperty('sling:resourceType') >> Mock(Property) {
                            getString() >> 'cq/security/components/profile'
                        }
                        getProperties() >> Mock(PropertyIterator) {
                            toList() >> []
                        }
                    } >>
                    Mock(Node) {
                        getName() >> "/home/users/u/user/.tokens"
                        getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                            getString() >> 'rep:Unstructured'
                        }
                        getProperties() >> Mock(PropertyIterator) {
                            toList() >> []
                        }
                    }  >>
                    Mock(Node) {
                        getName() >> "/home/users/u/user/rep:policy"
                        getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                            getString() >> 'rep:ACL'
                        }
                        getProperties() >> Mock(PropertyIterator) {
                            toList() >> []
                        }
                    }
            }
        }

        then:
        otherNodeDecorator.getRequiredChildNodes().size() == 2

        and: "If no child nodes, getRequiredChildNodes() returns an empty collection"

        when:
        final yetAnotherNodeDecorator = Spy(JCRNodeDecorator, constructorArgs: [nodeWithMandatoryChildren]) {
            hasMandatoryChildNodes() >> false
            isAuthorizableType() >> false
        }

        then:
        yetAnotherNodeDecorator.getRequiredChildNodes() == []
    }


    def "Can adapt the decorator back to the wrapped node"() {
        given:
        final node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }
        final nodeDecorator = new JCRNodeDecorator(node)

        expect:
        (nodeDecorator as Node) == node
        (nodeDecorator as JCRNodeDecorator) == nodeDecorator
    }

    def "Get modified date for a node"() {
        given:
        final node = Mock(Node) {
            hasProperty(JCR_LASTMODIFIED) >> jcrModifiedPresent
            getProperty(JCR_LASTMODIFIED) >> Mock(Property) {
                getDate() >> jcrModifiedDate
            }
            hasProperty("cq:lastModified") >> lastModifiedPresent
            getProperty("cq:lastModified") >> Mock(Property) {
                getDate() >> cqModifiedDate
            }
            hasProperty(JCR_CREATED) >> jcrCreatedPresent
            getProperty(JCR_CREATED) >> Mock(Property) {
                getDate() >> jcrCreatedDate
            }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:unstructured'
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }
        final nodeDecorator = new JCRNodeDecorator(node)

        expect:
        nodeDecorator.getModifiedOrCreatedDate() == modifiedDate

        where:
        jcrModifiedPresent  |   lastModifiedPresent |   jcrCreatedPresent   | modifiedDate
        true                |   true                |   false               | jcrModifiedDate.time
        false               |   true                |   true                | cqModifiedDate.time
        false               |   false               |   true                | jcrCreatedDate.time
        false               |   false               |   false               | null
    }

    def "isAuthorizablePart()"() {
        given:
        final nodeThatIsPart = Mock(Node) {
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
            getParent() >> Mock(Node) {
                getProperties() >> Mock(PropertyIterator) {
                    toList() >> []
                }
                getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                    getString() >> "nt:unstructured"
                }
                getParent() >> Mock(Node) {
                    getProperties() >> Mock(PropertyIterator) {
                        toList() >> []
                    }
                    getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                        getString() >> "rep:User"
                    }
                }
            }
        }

        final nodeThatIsNotPart = Mock(Node) {
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
            getParent() >> Mock(Node) {
                getProperties() >> Mock(PropertyIterator) {
                    toList() >> []
                }
                getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                    getString() >> "nt:unstructured"
                }
                getParent() >> { throw new ItemNotFoundException() }
            }
        }

        when:
        final JCRNodeDecorator jcrNodeDecorator = new JCRNodeDecorator(nodeThatIsPart)

        then:
        jcrNodeDecorator.isAuthorizablePart()

        and: "!isAuthorizablePart()"

        when:
        final JCRNodeDecorator jcrNodeDecoratorTwo = new JCRNodeDecorator(nodeThatIsNotPart)

        then:
        !jcrNodeDecoratorTwo.isAuthorizablePart()
    }

    @Unroll
    def "isAuthorizableType() for primary type #primaryType is expected #expected"() {
        given:
        final node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> primaryType
            }
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when:
        final JCRNodeDecorator jcrNodeDecorator = new JCRNodeDecorator(node)

        then:
        jcrNodeDecorator.isAuthorizableType() == expected

        where:
        primaryType   | expected
        'rep:User'    | true
        'rep:Group'   | true
        'unknown'     | false
    }


    @Unroll
    def "isLoginToken() for primary type #primaryType and name #name is expected #expected"() {
        given:
        final node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> primaryType
            }
            getName() >> name
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when:
        final JCRNodeDecorator jcrNodeDecorator = new JCRNodeDecorator(node)

        then:
        jcrNodeDecorator.isLoginToken() == expected

        where:
        primaryType        | name                                | expected
        'rep:Unstructured' | '/home/users/u/user/.tokens'        | true
        'nt:unstructured'  | '/home/users/u/user/.tokens'        | false
        'rep:Unstructured' | '/home/users/u/user/other'          | false
        'rep:Token'        | '/home/users/u/user/.tokens/token'  | true
        'unknown'          | 'unknown'                           | false
    }


    def "equals()"() {
        given:
        final JCRNodeDecorator decoratorOne = new JCRNodeDecorator(Mock(Node){
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
            getName() >> 'decoratorOne'
        })

        final JCRNodeDecorator otherDecoratorOneInstance = new JCRNodeDecorator(Mock(Node){
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
            getName() >> 'decoratorOne'
        })

        final JCRNodeDecorator decoratorTwo = new JCRNodeDecorator(Mock(Node){
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
            getName() >> 'decoratorTwo'
        })

        expect:
        decoratorOne.equals(decoratorOne)
        decoratorOne.equals(otherDecoratorOneInstance)
        !decoratorOne.equals(decoratorTwo)
        !decoratorOne.equals(Mock(Node))
    }


    def "NoRootInclusionPolicy behavior"() {
        given:
        final rootNode = Mock(Node) {
            getName() >> "/path/root"
            getProperties() >> Mock(PropertyIterator) {
                toList() >> []
            }
        }

        when:
        final NoRootInclusionPolicy policy = new NoRootInclusionPolicy(rootNode)

        then:
        policy.include(node) == expectedValue

        where:
        node  << [
            Mock(Node) {
                getName() >> "/path/root"
                getProperties() >> Mock(PropertyIterator) {
                    toList() >> []
                }
            },
            Mock(Node) {
                getName() >> "/path/root/node"
                getDefinition() >> Mock(NodeDefinition) {
                    isMandatory() >> false
                }
                getProperties() >> Mock(PropertyIterator) {
                    toList() >> []
                }
            },
            Mock(Node) {
                getName() >> "/path/root/node"
                getDefinition() >> Mock(NodeDefinition) {
                    isMandatory() >> true
                }
                getProperties() >> Mock(PropertyIterator) {
                    toList() >> []
                }
            }
        ]
        expectedValue << [false, true, false]
    }
}
