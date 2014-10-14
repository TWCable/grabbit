package com.twc.webcms.sync.server.services

import com.twc.jackalope.NodeBuilder as FakeNodeBuilder
import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Node as JcrNode

import static com.twc.jackalope.JCRBuilder.node
import static com.twc.jackalope.JCRBuilder.property

@Subject(JcrContentRecursiveIterator)
class JcrContentRecursiveIteratorSpec extends Specification {

    def "Can create a non-recursive iterator for a node"() {
        given:
        FakeNodeBuilder fakeNodeBuilder =
                node("page",
                        node("jcr:content",
                                property("jcr:data", "foo" )
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
