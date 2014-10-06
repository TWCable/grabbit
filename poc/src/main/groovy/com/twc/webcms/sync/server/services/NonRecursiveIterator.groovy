package com.twc.webcms.sync.server.services

import groovy.transform.CompileStatic

import javax.jcr.NodeIterator
import javax.jcr.Node as JcrNode

/**
 * Custom Node Iterator that will iterate through a list of Nodes containing of the root node and its children
 * TODO: http://jira.corp.mystrotv.com/browse/WEBCMS-14210 : Should account for cases where a node (root node or any
 * of the children) has a sub-node of "jcr:content"
 */
@CompileStatic
final class NonRecursiveIterator implements Iterator<JcrNode> {

    private boolean doneRoot
    private JcrNode root
    private NodeIterator children

    public NonRecursiveIterator(JcrNode root) {
        this.root = root
        this.doneRoot = false
        this.children = root.nodes
    }


    @Override
    boolean hasNext() {
        !doneRoot || children.hasNext()
    }

    @Override
    JcrNode next() {
        if(!doneRoot){
            doneRoot = true
            return root
        }
        return children.nextNode()
    }

    @Override
    void remove() {
        throw new UnsupportedOperationException("Not supported.")
    }
}
