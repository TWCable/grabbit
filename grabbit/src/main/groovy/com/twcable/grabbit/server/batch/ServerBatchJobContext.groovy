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

package com.twcable.grabbit.server.batch

import groovy.transform.CompileStatic

import javax.jcr.Node as JcrNode
import javax.servlet.ServletOutputStream

/**
 * Helper class that wraps a {@link ThreadLocal < ClientBatchJobContext >} variable used to store {@link ServletOutputStream}
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
