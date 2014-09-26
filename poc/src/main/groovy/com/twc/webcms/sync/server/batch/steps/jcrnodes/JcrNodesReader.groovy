package com.twc.webcms.sync.server.batch.steps.jcrnodes

import groovy.transform.CompileStatic
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException

import javax.annotation.Nonnull
import javax.jcr.Node as JcrNode

/**
 * A Custom ItemReader that provides the "next" Node from the {@link JcrNodesReader#nodeIterator}.
 * Returns null to indicate that all Items have been read.
 */
@CompileStatic
class JcrNodesReader implements ItemReader<JcrNode> {

    private Iterator<JcrNode> nodeIterator

    /**
     * {@link JcrNodesReader#nodeIterator} must be set before using JcrNodesReader using this method
     * @param the nodeIterator for current execution
     */
    public void setNodeIterator(@Nonnull Iterator<JcrNode> nodeIterator) {
        if(nodeIterator == null) throw new IllegalArgumentException("nodeIterator == null")
        this.nodeIterator = nodeIterator
    }

    @Override
    JcrNode read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if(nodeIterator == null) throw new IllegalStateException("nodeIterator must be set.")

        if(nodeIterator.hasNext()) {
            nodeIterator.next()
        }
        else {
            null
        }
    }
}
