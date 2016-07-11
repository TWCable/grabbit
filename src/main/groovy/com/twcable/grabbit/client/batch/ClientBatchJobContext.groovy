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

package com.twcable.grabbit.client.batch

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.annotation.Nonnull
import javax.jcr.Session

/**
 * Helper class that wraps a {@link ThreadLocal} variable used to store {@link InputStream}
 * and {@link Session} in ThreadLocal.
 */
@CompileStatic
@Slf4j
class ClientBatchJobContext {

    private static final ThreadLocal<ClientBatchJobContext> _THREAD_LOCAL = new ThreadLocal<ClientBatchJobContext>()

    private final InputStream inputStream
    private final Session session

    private ClientBatchJobContext(InputStream inputStream, Session session) {
        this.inputStream = inputStream
        this.session = session
    }

    /**
     * @return the job's JCR session, or null if it has not been started/set
     */
    static Session getSession() {
        _THREAD_LOCAL.get()?._getSession()
    }

    /**
     * @return the job's HTTP input stream to the server, or null if it has not been opened/set
     */
    static InputStream getInputStream() {
        _THREAD_LOCAL.get()?._getInputStream()
    }

    /**
     * Sets the HTTP input stream from the server for this job. Will only accept being set once per job.  Multiple calls to setInputStream in the same job will be ignored
     * @param inputStream
     */
    static void setInputStream(@Nonnull final InputStream inputStream) {
        final ClientBatchJobContext currentContext = _THREAD_LOCAL.get()
        if(currentContext?._getInputStream()) {
            return
        }
        _THREAD_LOCAL.set(new ClientBatchJobContext(inputStream, currentContext?._getSession()))
    }

    /**
     * Sets the JCR session for the current job. Will only accept being set once per job. Multiple calls to setSession in the same job will be ignored
     * @param session for this job
     */
    static void setSession(@Nonnull final Session session) {
        final ClientBatchJobContext currentContext = _THREAD_LOCAL.get()
        if(currentContext?._getSession()) {
            return
        }
        _THREAD_LOCAL.set(new ClientBatchJobContext(currentContext?._getInputStream(), session))
    }

    /**
     * Closes the current JCR session for this job
     */
    static void closeSession() {
        final ClientBatchJobContext currentContext = _THREAD_LOCAL.get()
        final Session thisSession = currentContext?._getSession()
        if(thisSession?.isLive()) {
            thisSession.logout()
        }
    }

    /**
     * Close the current input stream associated with this job
     */
    static void closeInputStream() {
        final ClientBatchJobContext currentContext = _THREAD_LOCAL.get()
        final InputStream inputStream = currentContext?._getInputStream()
        try {
            inputStream?.close()
        } catch(IOException ex) {
            log.error "${this.class.canonicalName} Attempt to close jobs input stream resulted in an IOException. Either something went wrong closing the stream, or the stream was already closed"
            log.error ex.toString()
        }
    }

    /**
     * Removes information stored on this job's thread
     */
    static void cleanup() {
        closeSession()
        closeInputStream()
        _THREAD_LOCAL.remove()
    }

    private InputStream _getInputStream() {
        inputStream
    }

    private Session _getSession() {
        session
    }

}
