package com.twc.grabbit.server.batch

import groovy.transform.CompileStatic

import javax.jcr.Node as JcrNode
import javax.servlet.ServletOutputStream

/**
 * Helper class that wraps a {@link ThreadLocal<ClientBatchJobContext>} variable used to store {@link ServletOutputStream}
 * , {@link ServerBatchJobContext#namespacesIterator} and {@link ServerBatchJobContext#nodeIterator} in ThreadLocal.
 */
@CompileStatic
class ServerBatchJobContext {

    static final ThreadLocal<ServerBatchJobContext> THREAD_LOCAL = new ThreadLocal<ServerBatchJobContext>()

    final ServletOutputStream servletOutputStream
    final Iterator<Map.Entry<String, String>> namespacesIterator
    final Iterator<JcrNode> nodeIterator

    ServerBatchJobContext(ServletOutputStream servletOutputStream, Iterator<Map.Entry<String, String>> namespacesIterator,
                          Iterator<JcrNode> nodeIterator) {
        this.servletOutputStream = servletOutputStream
        this.namespacesIterator = namespacesIterator
        this.nodeIterator = nodeIterator
    }

}
