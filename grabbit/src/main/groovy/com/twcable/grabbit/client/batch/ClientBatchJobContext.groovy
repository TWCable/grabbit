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
