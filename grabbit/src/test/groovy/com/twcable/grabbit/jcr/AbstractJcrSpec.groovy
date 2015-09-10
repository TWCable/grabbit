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

import com.twcable.jackalope.impl.jcr.ValueImpl
import org.apache.jackrabbit.JcrConstants
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter
import spock.lang.Specification

import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.PropertyIterator
import javax.jcr.nodetype.NodeDefinition
import javax.jcr.nodetype.NodeType

import static javax.jcr.PropertyType.STRING

abstract class AbstractJcrSpec extends Specification {

    Node createNode(String path, boolean isMandatory, String primaryType) {
        createNode(path, isMandatory, primaryType, [])
    }


    Node createNode(String path, boolean isMandatory, String primaryType, Collection<Node> children) {
        def nodeDefinition = isMandatory ? mandatoryNodeDefinition() : nonMandatoryNodeDefinition()

        def childDefinitions = children.collect { it.getDefinition() } as List<NodeDefinition>

        def node = Mock(Node) {
            getPath() >> path
            getDefinition() >> nodeDefinition
            getProperties() >> propertyIterator(primaryTypeProperty(primaryType))
            getPrimaryNodeType() >> Mock(NodeType) {
                getChildNodeDefinitions() >> childDefinitions.toArray()
            }
        }

        children.each { Node child ->
            child.getParent() >> node
        }
        node.getNodes() >> new NodeIteratorAdapter(children.iterator())

        return node
    }

    protected NodeDefinition mandatoryNodeDefinition() {
        return Mock(NodeDefinition) {
            isMandatory() >> true
        }
    }

    protected NodeDefinition nonMandatoryNodeDefinition() {
        return Mock(NodeDefinition) {
            isMandatory() >> false
        }
    }

    protected Property primaryTypeProperty(String propertyValue) {
        return Mock(Property) {
            getType() >> STRING
            getName() >> JcrConstants.JCR_PRIMARYTYPE
            getValue() >> new ValueImpl(propertyValue)
        }
    }

    protected static PropertyIterator propertyIterator(Property... properties) {
        new PropertyIteratorAdapter(properties.iterator())
    }

}
