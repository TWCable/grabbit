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

@Subject(ExcludePathNodeIterator)
class ExcludePathNodeIteratorSpec extends Specification {

    def "Can create an exclusion iterator for a node with exclusions"() {
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
        final nodeIterator = new ExcludePathNodeIterator(new RootNodeWithMandatoryIterator(rootNode), ["page/jcr:content"] as Collection)
        final root = nodeIterator.next()
        def childNodes = nodeIterator.collect()

        then:
        root.getName() == "page"
        childNodes.size() == 2
        childNodes.find { JcrNode node -> node.getName() == "jcr:content" } == null

    }

    def "Can create an exclusion iterator without exclusions"() {
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
        final Iterator<JcrNode> nodeIterator = new ExcludePathNodeIterator(new RootNodeWithMandatoryIterator(rootNode), (Collection<String>)Collections.EMPTY_LIST)
        final JcrNode root = nodeIterator.next()
        final JcrNode firstChild = nodeIterator.next()
        then:
        root.getName() == "page"
        firstChild.getName() == "jcr:content"
    }

    def "Can create an exclusion iterator for a node with exclusions and similar siblings"() {
        given:
        FakeNodeBuilder fakeNodeBuilder =
                node("page",
                        node("jcr:content",
                                property("jcr:data", "foo")
                        ),
                        node("childpage",
                                property("jcr:primaryType", "cq:Page"),
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
        final nodeIterator = new ExcludePathNodeIterator(new RootNodeWithMandatoryIterator(rootNode), ["page/childpage"] as Collection)
        final root = nodeIterator.next()
        def childNodes = nodeIterator.collect()

        then:
        root.getName() == "page"
        childNodes.size() == 3
        childNodes.find { JcrNode node -> node.getName() == "childpage" } == null
    }
}
