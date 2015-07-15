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

package com.twcable.grabbit.server.batch.steps.jcrnodes

import com.twcable.grabbit.proto.NodeProtos
import com.twcable.jackalope.NodeBuilder as FakeNodeBuilder
import com.twcable.jackalope.impl.jcr.ValueImpl
import org.apache.jackrabbit.JcrConstants
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter
import spock.lang.Specification
import spock.lang.Subject
import javax.jcr.Node as JcrNode
import javax.jcr.Property as JcrProperty
import javax.jcr.PropertyIterator
import javax.jcr.nodetype.NodeDefinition
import javax.jcr.nodetype.NodeType

import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property
import static javax.jcr.PropertyType.LONG
import static javax.jcr.PropertyType.STRING

@Subject(JcrNodesProcessor)
class JcrNodesProcessorSpec extends Specification {

    def "Can marshall a JCR Node to a Protobuf Message"() {
        given:
        FakeNodeBuilder fakeNodeBuilder =
                node("default.groovy",
                        node("jcr:content",
                                property("jcr:data", "foo")
                        ),
                        property("jcr:primaryType", JcrConstants.NT_FILE),
                        property("jcr:lastModified", "Date"),
                        property("multiValueLong", [1L, 2L, 4L] as Object[]),
                        property("multiValueString", ["a", "b", "c"] as Object[]),

                )
        JcrNode aJcrNode = fakeNodeBuilder.build()

        when:
        JcrNodesProcessor jcrNodesProcessor = new JcrNodesProcessor()
        NodeProtos.Node nodeProto = jcrNodesProcessor.process(aJcrNode)
        NodeProtos.Property propertyLong = nodeProto.properties.propertyList.find { it.name == "multiValueLong" }
        NodeProtos.Property propertyString = nodeProto.properties.propertyList.find { it.name == "multiValueString" }

        then:
        nodeProto != null
        nodeProto.hasProperties()

        propertyLong.hasValues()
        !propertyLong.hasValue()
        propertyLong.values.valueCount == 3
        propertyLong.type == LONG


        propertyString.hasValues()
        !propertyString.hasValue()
        propertyString.values.valueCount == 3
        propertyString.type == STRING
    }

    def "Can marshall a JCR Node nt:file with mandatory child node nt:resource to a Protobuf Message "() {
        given:
        def imageFile = "/content/geometrixx-outdoors/jcr:content/image/file/jcr:content/dam:thumbnails/dam:thumbnail_600.png"
        def parentNode = createNode(imageFile, false, JcrConstants.NT_FILE,
                [createNode("${imageFile}/jcr:content", true, JcrConstants.NT_RESOURCE)])

        when:
        NodeProtos.Node nodeProto = new JcrNodesProcessor().process(parentNode)

        then:
        nodeProto.name == imageFile
        nodeProto.properties.propertyList.first().value.stringValue == JcrConstants.NT_FILE
        nodeProto.mandatoryChildNodeList.size() == 1
        nodeProto.mandatoryChildNodeList.first().name == "${imageFile}/jcr:content"
        nodeProto.mandatoryChildNodeList.first().properties.propertyList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().properties.propertyList.first().value.stringValue == JcrConstants.NT_RESOURCE
    }

    def "Can marshall a JCR Node nt:file with mandatory child node nt:unstructured to a Protobuf Message "() {
        given:
        def thumbnailFile = "/content/dam/geometrixx-outdoors/activities/jcr:content/folderThumbnail"
        def parentNode = createNode(thumbnailFile, false, JcrConstants.NT_FILE,
                [createNode("${thumbnailFile}/jcr:content", true, JcrConstants.NT_UNSTRUCTURED)])

        when:
        NodeProtos.Node nodeProto = new JcrNodesProcessor().process(parentNode)

        then:
        nodeProto.name == thumbnailFile
        nodeProto.properties.propertyList.first().value.stringValue == JcrConstants.NT_FILE
        nodeProto.mandatoryChildNodeList.size() == 1
        nodeProto.mandatoryChildNodeList.first().name == "${thumbnailFile}/jcr:content"
        nodeProto.mandatoryChildNodeList.first().properties.propertyList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().properties.propertyList.first().value.stringValue == JcrConstants.NT_UNSTRUCTURED
    }

    def "Skip mandatory child node nt:resource as parent has already process"() {
        given:
        def imageFile = "/content/geometrixx-outdoors/jcr:content/image/file/jcr:content/dam:thumbnails/dam:thumbnail_600.png"
        def childrenNode = createNode("${imageFile}/jcr:content", true, JcrConstants.NT_RESOURCE)

        when:
        NodeProtos.Node nodeProto = new JcrNodesProcessor().process(childrenNode)

        then:
        nodeProto == null
    }

    def "Example not found and extreme case where grand child node is also required"() {
        given:
        def thumbnailFile = "/content/dam/geometrixx-outdoors/activities/jcr:content/folderThumbnail"
        def parentNode = createNode(thumbnailFile, false, JcrConstants.NT_FILE,
                [createNode("${thumbnailFile}/jcr:content", true, JcrConstants.NT_RESOURCE,
                    [createNode("${thumbnailFile}/jcr:content/metadata", true, JcrConstants.NT_UNSTRUCTURED)])])

        when:
        NodeProtos.Node nodeProto = new JcrNodesProcessor().process(parentNode)

        then:
        nodeProto.name == thumbnailFile
        nodeProto.properties.propertyList.first().value.stringValue == JcrConstants.NT_FILE
        nodeProto.mandatoryChildNodeList.size() == 1
        nodeProto.mandatoryChildNodeList.first().name == "${thumbnailFile}/jcr:content"
        nodeProto.mandatoryChildNodeList.first().properties.propertyList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().properties.propertyList.first().value.stringValue == JcrConstants.NT_RESOURCE

        nodeProto.mandatoryChildNodeList.first().mandatoryChildNodeList.first().name == "${thumbnailFile}/jcr:content/metadata"
        nodeProto.mandatoryChildNodeList.first().mandatoryChildNodeList.first().properties.propertyList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().mandatoryChildNodeList.first().properties.propertyList.first().value.stringValue == JcrConstants.NT_UNSTRUCTURED
    }

    private JcrNode createNode(String path, boolean isMandatory, String primaryType, Collection<JcrNode> children = []) {
        def nodeDefinition = isMandatory ? mandatoryNodeDefinition() : nonMandatoryNodeDefinition()

        def childDefinitions = children.collect {it.getDefinition()} as List<NodeDefinition>

        def node = Mock(JcrNode) {
            getPath() >> path
            getDefinition() >> nodeDefinition
            getProperties() >> propertyIterator(primaryTypeProperty(primaryType))
            getPrimaryNodeType() >> Mock(NodeType) {
                getChildNodeDefinitions() >> childDefinitions.toArray()
            }
        }

        children.each { JcrNode child ->
            child.getParent() >> node
        }
        node.getNodes() >> new NodeIteratorAdapter(children.iterator())

        return node
    }

    private NodeDefinition mandatoryNodeDefinition() {
        return Mock(NodeDefinition) {
            isMandatory() >> true
        }
    }

    private NodeDefinition nonMandatoryNodeDefinition() {
        return Mock(NodeDefinition) {
            isMandatory() >> false
        }
    }

    private JcrProperty primaryTypeProperty(String propertyValue) {
        return Mock(JcrProperty) {
            getType() >> STRING
            getName() >> JcrConstants.JCR_PRIMARYTYPE
            getValue() >> new ValueImpl(propertyValue)
        }
    }

    private static PropertyIterator propertyIterator(JcrProperty... properties) {
        new PropertyIteratorAdapter(properties.iterator())
    }
}
