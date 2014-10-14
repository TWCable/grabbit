package com.twc.webcms.sync.server.services

import groovy.transform.CompileStatic
import org.apache.jackrabbit.commons.flat.TreeTraverser

import javax.jcr.NodeIterator
import javax.jcr.Node as JcrNode

/**
 * Custom Node Iterator that will iterate through a list of Nodes containing of the root node and its children
 * Accounts for cases where a node (root node or any of the children) has a sub-node of "jcr:content"
 * and will recursively pull all content from jcr:content type nodes
 */
@CompileStatic
final class JcrContentRecursiveIterator implements Iterator<JcrNode> {

    private boolean doneRoot
    private JcrNode root
    private NodeIterator children
    private Iterator<JcrNode> recursiveNodes
    private JcrNode previous

    public JcrContentRecursiveIterator(JcrNode root) {
        this.root = root
        this.doneRoot = false
        this.children = root.nodes
    }


    @Override
    boolean hasNext() {
        !doneRoot || children.hasNext() || recursiveNodes.hasNext()
    }

    @Override
    JcrNode next() {

        //always process root first
        if(!doneRoot){
            doneRoot = true
            return root
        }

        //if there are recursive nodes to traverse do those next
        if(recursiveNodes) {
            return recursiveNodes.next()
        }

        //the direct children of root
        JcrNode childNode = children.nextNode()

        //store the previous node to determine if we need to come back to process jcr contents
        this.previous = childNode

        //if the childNode is the JCR node, we need all it's children
        if(childNode.name.equalsIgnoreCase("jcr:content")){
            this.recursiveNodes = TreeTraverser.nodeIterator(childNode)
            return recursiveNodes.next()
        }

        //if the previously processed childNode contains a JCR node child we need all of it's contents as well
        if(previous.hasNode("jcr:content")){
            JcrNode jcrNode = previous.getNode("jcr:content")
            this.recursiveNodes = TreeTraverser.nodeIterator(jcrNode)
            return previous
        }

        return childNode
    }

    @Override
    void remove() {
        throw new UnsupportedOperationException("Not supported.")
    }
}
