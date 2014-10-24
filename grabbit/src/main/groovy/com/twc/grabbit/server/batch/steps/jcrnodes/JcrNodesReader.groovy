package com.twc.grabbit.server.batch.steps.jcrnodes

import com.twc.grabbit.server.batch.ServerBatchJobContext
import groovy.transform.CompileStatic
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException

import javax.jcr.Node as JcrNode

/**
 * A Custom ItemReader that provides the "next" Node from the {@link ServerBatchJobContext#nodeIterator}.
 * Returns null to indicate that all Items have been read.
 */
@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
class JcrNodesReader implements ItemReader<JcrNode> {

    @Override
    JcrNode read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Iterator<JcrNode> nodeIterator = theNodeIterator()
        if(nodeIterator == null) throw new IllegalStateException("nodeIterator must be set.")

        if(nodeIterator.hasNext()) {
            nodeIterator.next()
        }
        else {
            null
        }
    }

    private Iterator<JcrNode> theNodeIterator() {
        ServerBatchJobContext serverBatchJobContext = ServerBatchJobContext.THREAD_LOCAL.get()
        serverBatchJobContext.nodeIterator
    }
}
