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

import com.twcable.grabbit.jcr.JCRNodeDecorator
import groovy.transform.CompileStatic
import groovy.transform.TailRecursive

import javax.jcr.Node as JcrNode

/**
 * Custom Node Iterator that will iterate over a rootNode node and its immediate children
 * taking into account cases where a node (rootNode node or any of the children) has any number of
 * mandatory subnodes
 */
@CompileStatic
final class RootNodeWithMandatoryIterator implements Iterator<JcrNode> {

    private boolean doneRoot
    private JCRNodeDecorator rootNode
    private Iterator<JCRNodeDecorator> immediateChildren
    private Iterator<JCRNodeDecorator> mandatoryWriteNodes


    public RootNodeWithMandatoryIterator(JcrNode root) {
        this.rootNode = new JCRNodeDecorator(root)
        this.doneRoot = false
        //Get all immediate children that are not mandatory write nodes.  We will handle those by iterating over mandatoryWriteNodes
        immediateChildren = getNonMandatoryChildren(this.rootNode).iterator()
        //Calls a tail recursive method, gathering all required nodes
        mandatoryWriteNodes = getMandatoryChildren(rootNode, []).iterator()
    }


    @Override
    boolean hasNext() {
        !doneRoot || immediateChildren.hasNext() || mandatoryWriteNodes.hasNext()
    }


    @Override
    JcrNode next() {
        if(!hasNext()) throw new NoSuchElementException("No more elements in Iterator")

        //always process rootNode first
        if (!doneRoot) {
            doneRoot = true
            return rootNode
        }

        if(immediateChildren.hasNext()) {
            return immediateChildren.next()
        }

        return mandatoryWriteNodes.next()
    }


    @Override
    void remove() {
        throw new UnsupportedOperationException("Not supported.")
    }


    private static Collection<JCRNodeDecorator> getNonMandatoryChildren(final JCRNodeDecorator node) {
        node.getImmediateChildNodes().findAll { !it.isMandatoryNode() }
    }


    @TailRecursive
    private static Collection<JCRNodeDecorator> getMandatoryChildren(final JCRNodeDecorator currentNode, final Collection<JCRNodeDecorator> nodesToAdd) {
        if(!currentNode.hasMandatoryChildNodes()) {
            return nodesToAdd
        }

        final mandatoryNodes = currentNode.getRequiredChildNodes()

        return mandatoryNodes.collectMany { JCRNodeDecorator mandatoryNode ->
            return getMandatoryChildren(mandatoryNode, (nodesToAdd << mandatoryNode))
        }
    }
}
