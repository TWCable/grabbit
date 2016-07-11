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

package com.twcable.grabbit.server.services

import com.twcable.jackalope.NodeBuilder as FakeNodeBuilder
import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Node
import javax.jcr.Node as JcrNode
import javax.jcr.NodeIterator
import javax.jcr.nodetype.NodeDefinition
import javax.jcr.nodetype.NodeType

import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property

@Subject(RootNodeWithMandatoryIterator)
class RootNodeWithMandatoryIteratorSpec extends Specification {

    def "Iterates fine through nodes that have no mandatory nodes"() {
        given:
        FakeNodeBuilder fakeNodeBuilder =
            node("page",
                node("jcr:content",
                    property("jcr:data", "foo")
                ),
                node("childpage1",
                    property("jcr:primaryType", "cq:Page"),
                ),
                node("childpage2",
                    property("jcr:primaryType", "cq:Page"),
                )
            )
        JcrNode rootNode = fakeNodeBuilder.build()

        when:
        final Iterator<JcrNode> nodeIterator = new RootNodeWithMandatoryIterator(rootNode)
        final JcrNode root = nodeIterator.next()
        final JcrNode firstChild = nodeIterator.next()
        then:
        root.getName() == "page"
        firstChild.getName() == "jcr:content"

    }


    //TODO: When better node definition support is added to Jackalope, create a more robust/graceful test
    def "Test mandatory node gathering capability"() {
        /**
         * rootNode
         *     _child1 (mandatory)
         *         _child3 (mandatory)
         *     _child2 (not mandatory)
         */
        given:
        final nodeTypeWithMandatory =  Mock(NodeType) {
            getChildNodeDefinitions() >> [
                Mock(NodeDefinition) {
                    isMandatory() >> true
                }
            ]
        }

        final nodeTypeNoMandatory =  Mock(NodeType) {
            getChildNodeDefinitions() >> [
                    Mock(NodeDefinition) {
                        isMandatory() >> false
                    }
            ]
        }

        final child3 = Mock(Node) {
            getDefinition() >> Mock(NodeDefinition) {
                isMandatory() >> true
            }
            getPrimaryNodeType() >> nodeTypeNoMandatory
        }

        final child1 = Mock(Node) {
            getDefinition() >> Mock(NodeDefinition) {
                isMandatory() >> true
            }
            getPrimaryNodeType() >> nodeTypeWithMandatory
            getNodes() >> Mock(NodeIterator) {
                hasNext() >>> [true, false]
                next() >> child3
            }
        }

        final child2 = Mock(Node) {
            getDefinition() >> Mock(NodeDefinition) {
                isMandatory() >> false
            }
        }

        final rootNode = Mock(Node) {
            getNodes() >> Mock(NodeIterator){
                hasNext() >>> [true, true, false]
                next() >>> [child1, child2]
            }
            getPrimaryNodeType() >> nodeTypeWithMandatory
        }

        when:
        final iterator = new RootNodeWithMandatoryIterator(rootNode)
        final elements = iterator.toList()

        then:
        elements.size() == 4
    }

}
