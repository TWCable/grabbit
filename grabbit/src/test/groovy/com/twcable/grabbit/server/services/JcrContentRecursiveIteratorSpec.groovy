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

import javax.jcr.Node as JcrNode

import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property

@Subject(JcrContentRecursiveIterator)
class JcrContentRecursiveIteratorSpec extends Specification {

    def "Can create a non-recursive iterator for a node"() {
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
        final Iterator<JcrNode> nodeIterator = new JcrContentRecursiveIterator(rootNode)
        final JcrNode root = nodeIterator.next()
        final JcrNode firstChild = nodeIterator.next()
        then:
        root.name == "page"
        firstChild.name == "jcr:content"

    }


    def "Can create iterator that is recursive on jcr content nodes"() {
        given:
        FakeNodeBuilder fakeNodeBuilder =
            node("page",
                node("jcr:content",
                    node("subnode1",
                        property("nt:unstructured")),
                    node("subnode2",
                        property("nt:unstructured"))
                )
            )
        JcrNode rootNode = fakeNodeBuilder.build()

        when:
        final Iterator<JcrNode> nodeIterator = new JcrContentRecursiveIterator(rootNode)
        final JcrNode root = nodeIterator.next()
        final JcrNode jcrChildNode = nodeIterator.next()
        final JcrNode jcrSubNode1 = nodeIterator.next()
        final JcrNode jcrSubNode2 = nodeIterator.next()

        then:
        root.name == "page"
        jcrChildNode.name == "jcr:content"
        jcrSubNode1.name == "subnode1"
        jcrSubNode2.name == "subnode2"

    }

}
