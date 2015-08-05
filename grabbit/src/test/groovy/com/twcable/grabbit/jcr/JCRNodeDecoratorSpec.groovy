package com.twcable.grabbit.jcr

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

import com.day.cq.commons.jcr.JcrConstants
import spock.lang.Specification

import javax.jcr.Node
import javax.jcr.NodeIterator
import javax.jcr.Property
import javax.jcr.RepositoryException
import javax.jcr.nodetype.ItemDefinition
import javax.jcr.nodetype.NodeDefinition
import javax.jcr.nodetype.NodeType

import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@SuppressWarnings("GroovyAssignabilityCheck")
class JCRNodeDecoratorSpec extends Specification {

    def "null nodes are not allowed for JCRNodeDecorator construction"() {
        when:
        new JCRNodeDecorator(null)

        then:
        thrown(IllegalArgumentException)
    }


    def "setLastModified() when last modified can be set"() {
        given:
        Node node = Mock(Node) {
            getPrimaryNodeType() >> Mock(NodeType) {
                canSetProperty(JCR_LASTMODIFIED, _) >> { true }
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
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)
        nodeDecorator.setLastModified()

        then:
        notThrown(RepositoryException)
    }


    def "getPrimaryType()"() {
        given:
        Node node = Mock(Node) {
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> { JcrConstants.NT_FILE }
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)

        then:
        nodeDecorator.getPrimaryType() == JcrConstants.NT_FILE
    }


    def "isRequiredNode()"() {
        given:
        Node node = Mock(Node) {
            getDefinition() >> Mock(NodeDefinition) {
                isMandatory() >> isMandatory
            }
        }

        when:
        final nodeDecorator = new JCRNodeDecorator(node)

        then:
        nodeDecorator.isRequiredNode() == isMandatory

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
        Node node = Mock(Node) {
            getNodes() >> Mock(NodeIterator) {
                hasNext() >>> true >> false
                next() >> Mock(Node) {
                    getDefinition() >> Mock(NodeDefinition) {
                        isMandatory() >> true
                    }
                }
            }
        }

        when: "The node has children"
        final nodeDecorator = Spy(JCRNodeDecorator, constructorArgs: [node]) {
            hasMandatoryChildNodes() >> true
        }

        then:
        nodeDecorator.getRequiredChildNodes().size() == 1

        and: "If no child nodes, getRequiredChildNodes() returns null"

        when:
        final otherNodeDecorator = Spy(JCRNodeDecorator, constructorArgs: [Mock(Node)]) {
            hasMandatoryChildNodes() >> false
        }

        then:
        otherNodeDecorator.getRequiredChildNodes() == null
    }


    def "Can adapt the decorator back to the wrapped node"() {
        given:
        final node = Mock(Node)
        final nodeDecorator = new JCRNodeDecorator(node)

        expect:
        (nodeDecorator as Node) == node
    }
}
