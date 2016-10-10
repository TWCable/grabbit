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
import javax.jcr.ItemNotFoundException
import org.apache.jackrabbit.JcrConstants
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter
import spock.lang.Specification
import spock.lang.Subject
import javax.jcr.Node as JcrNode
import javax.jcr.Property
import javax.jcr.Property as JcrProperty
import javax.jcr.PropertyIterator
import javax.jcr.nodetype.NodeDefinition
import javax.jcr.nodetype.NodeType

import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property
import static javax.jcr.PropertyType.LONG
import static javax.jcr.PropertyType.STRING
import org.joda.time.DateTime
import javax.jcr.NodeIterator
import spock.lang.Shared
import spock.lang.Unroll

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

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
        NodeProtos.Property propertyLong = nodeProto.propertiesList.find { it.name == "multiValueLong" }
        NodeProtos.Property propertyString = nodeProto.propertiesList.find { it.name == "multiValueString" }

        then:
        nodeProto != null
        nodeProto.propertiesCount > 0

        propertyLong.valuesCount == 3
        propertyLong.type == LONG

        propertyString.valuesCount == 3
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
        nodeProto.propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_FILE
        nodeProto.mandatoryChildNodeList.size() == 1
        nodeProto.mandatoryChildNodeList.first().name == "${imageFile}/jcr:content"
        nodeProto.mandatoryChildNodeList.first().propertiesList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_RESOURCE
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
        nodeProto.propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_FILE
        nodeProto.mandatoryChildNodeList.size() == 1
        nodeProto.mandatoryChildNodeList.first().name == "${thumbnailFile}/jcr:content"
        nodeProto.mandatoryChildNodeList.first().propertiesList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_UNSTRUCTURED
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
        nodeProto.propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_FILE
        nodeProto.mandatoryChildNodeList.size() == 1
        nodeProto.mandatoryChildNodeList.first().name == "${thumbnailFile}/jcr:content"
        nodeProto.mandatoryChildNodeList.first().propertiesList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_RESOURCE

        nodeProto.mandatoryChildNodeList.first().mandatoryChildNodeList.first().name == "${thumbnailFile}/jcr:content/metadata"
        nodeProto.mandatoryChildNodeList.first().mandatoryChildNodeList.first().propertiesList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().mandatoryChildNodeList.first().propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_UNSTRUCTURED
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
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> primaryType
            }
            getParent() >> { throw new ItemNotFoundException() }
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

    /* Node Processing & DeltaContent testing */

    def "Process a mandatory child node with no date properties but modified parent"() {
        //If a node has no date properties but is a mandatory child node whose parent has updated date properties
        //it should be processed and passed through as deltaContent
        given:
        DateTime currentDate = new DateTime()

        JcrNode node = Mock(JcrNode) {  //mocks a parent with updated lastModified property
            getPath() >> "testParent"
            getParent() >> { throw new ItemNotFoundException() }
            getProperties() >> propertyIterator(primaryTypeProperty(JcrConstants.NT_FILE))
            hasProperty(JcrConstants.JCR_LASTMODIFIED) >> true
            getProperty(JcrConstants.JCR_LASTMODIFIED) >> Mock(JcrProperty){
                getDate() >> currentDate.toCalendar(Locale.default)
            }
            getDefinition() >> Mock(NodeDefinition) {
                isMandatory() >> false //not mandatory child
            }
            getPrimaryNodeType() >> Mock(NodeType) {
                getChildNodeDefinitions() >> Mock(NodeDefinition) {
                    isMandatory() >> true //has mandatory child
                }
            }
            getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                getString() >> 'nt:file'
            }

            getNodes() >> Mock(NodeIterator) {
                hasNext() >>> true >> false
                next() >> Mock(JcrNode) {
                    getPath() >> "testMandatoryChild" //child node with no date properties
                    getProperties() >> propertyIterator(primaryTypeProperty(JcrConstants.NT_UNSTRUCTURED))
                    getDefinition() >> Mock(NodeDefinition) {
                        isMandatory() >> true //mandatory child node
                    }
                    getPrimaryNodeType() >> Mock(NodeType) {
                        getChildNodeDefinitions() >> Mock(NodeDefinition) {
                            isMandatory() >> false //has no children
                        }
                    }
                    getProperty(JCR_PRIMARYTYPE) >> Mock(Property) {
                        getString() >> 'nt:unstructured'
                    }
                }
            }
        }

        when:
        JcrNodesProcessor jcrNodesProcessor = new JcrNodesProcessor()
        DateTime dateTime = new DateTime(2015, 8, 4, 15, 24, 34, 961)
        jcrNodesProcessor.contentAfterDate = (dateTime.toString())
        NodeProtos.Node nodeProto = jcrNodesProcessor.process(node)

        then:
        nodeProto != null
        nodeProto.propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_FILE
        nodeProto.mandatoryChildNodeList.size() == 1
        nodeProto.mandatoryChildNodeList.first().name == "testMandatoryChild" //making sure child was processed
        nodeProto.mandatoryChildNodeList.first().propertiesList.first().name == JcrConstants.JCR_PRIMARYTYPE
        nodeProto.mandatoryChildNodeList.first().propertiesList.first().valuesList[0].stringValue == JcrConstants.NT_UNSTRUCTURED
    }


    @Shared
    DateTime oldDate = new DateTime(2015, 8, 4, 15, 24, 34, 961)

    @Unroll
    def "Process a node with old date properties: #propList"() {
        //tests for all situations with non-deltaContent nodes (not updated since last sync)
        when:
        JcrNodesProcessor jcrNodesProcessor = new JcrNodesProcessor()
        jcrNodesProcessor.setContentAfterDate(oldDate.plusDays(1).toString())
        NodeProtos.Node nodeProto = jcrNodesProcessor.process(aJcrNode)
        aJcrNode.properties.toList()

        then:
        nodeProto == null //not copying old data

        where:
        aJcrNode << [
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property(JcrConstants.JCR_CREATED, oldDate.toCalendar(Locale.default)),
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property(JcrConstants.JCR_LASTMODIFIED, oldDate.toCalendar(Locale.default))
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property("cq:lastModified", oldDate.toCalendar(Locale.default))
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property(JcrConstants.JCR_CREATED, oldDate.toCalendar(Locale.default)),
                        property(JcrConstants.JCR_LASTMODIFIED, oldDate.toCalendar(Locale.default))
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property(JcrConstants.JCR_CREATED, oldDate.toCalendar(Locale.default)),
                        property("cq:lastModified", oldDate.toCalendar(Locale.default))
                ).build(),
        ]
        propList = aJcrNode.properties.toList().findAll {it.name != JcrConstants.JCR_PRIMARYTYPE}.collectEntries { [(it.name): new DateTime(it.value.date.time.time)]}
    }

    @Shared
    DateTime currentDate = new DateTime()

    @Unroll
    def "Process a node with updated content and date properties: #propList"() {
        when:
        JcrNodesProcessor jcrNodesProcessor = new JcrNodesProcessor()
        jcrNodesProcessor.setContentAfterDate(oldDate.plusDays(1).toString())
        NodeProtos.Node nodeProto = jcrNodesProcessor.process(aJcrNode)
        aJcrNode.properties.toList()

        then:
        nodeProto != null //checking that new data is being copied
        nodeProto.propertiesCount > 0

        where:
        aJcrNode << [
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        //nothing is 'infinitely old' so we are syncing nodes without date properties
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property(JcrConstants.JCR_CREATED, currentDate.toCalendar(Locale.default)),
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property(JcrConstants.JCR_LASTMODIFIED, currentDate.toCalendar(Locale.default))
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property("cq:lastModified", currentDate.toCalendar(Locale.default))
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property(JcrConstants.JCR_CREATED, oldDate.toCalendar(Locale.default)),
                        property(JcrConstants.JCR_LASTMODIFIED, currentDate.toCalendar(Locale.default))
                ).build(),
                node("testComponent",
                        property(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                        property(JcrConstants.JCR_CREATED, oldDate.toCalendar(Locale.default)),
                        property("cq:lastModified", currentDate.toCalendar(Locale.default))
                ).build(),
        ]
        propList = aJcrNode.properties.toList().findAll {it.name != JcrConstants.JCR_PRIMARYTYPE}.collectEntries { [(it.name): new DateTime(it.value.date.time.time)]}
    }
}
