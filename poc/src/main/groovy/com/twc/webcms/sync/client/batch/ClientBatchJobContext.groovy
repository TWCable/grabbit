package com.twc.webcms.sync.client.batch

import groovy.transform.CompileStatic

import javax.jcr.Session

/**
 * Helper class that wraps a {@link ThreadLocal<ClientBatchJobContext>} variable used to store {@link InputStream}
 * and {@link Session} in ThreadLocal.
 */
@CompileStatic
class ClientBatchJobContext {

    static final ThreadLocal<ClientBatchJobContext> THREAD_LOCAL = new ThreadLocal<ClientBatchJobContext>()

    final InputStream inputStream
    final Session session

    ClientBatchJobContext(InputStream inputStream, Session session) {
        this.inputStream = inputStream
        this.session = session
    }

}
