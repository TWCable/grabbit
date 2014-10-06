package com.twc.webcms.sync.server.services

import com.twc.jackalope.NodeBuilder as FakeNodeBuilder
import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Node as JcrNode

import static com.twc.jackalope.JCRBuilder.node
import static com.twc.jackalope.JCRBuilder.property

@Subject(NonRecursiveIterator)
class NonRecursiveIteratorSpec extends Specification {

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
        final Iterator<JcrNode> nodeIterator = new NonRecursiveIterator(rootNode)
        final JcrNode root = nodeIterator.next()
        final JcrNode firstChild = nodeIterator.next()
        then:
        root.name == "page"
        firstChild.name == "jcr:content"

    }


}
