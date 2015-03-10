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

package com.twcable.grabbit.server.batch.steps.preprocessor

import com.twcable.grabbit.server.batch.ServerBatchJobContext
import groovy.transform.CompileStatic
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException

import static java.util.Map.Entry

/**
 * A Custom ItemReader that provides the "next" Namespace Entry from the {@link ServerBatchJobContext#namespacesIterator}.
 * Returns null to indicate that all Items have been read.
 */
@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
class PreprocessReader implements ItemReader<Entry<String, String>> {

    @Override
    Entry<String, String> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Iterator<Entry<String, String>> namespaces = theNamespaces()
        if (namespaces == null) throw new IllegalStateException("namespaces must be set.")

        if (namespaces.hasNext()) {
            namespaces.next()
        } else {
            null
        }
    }

    private Iterator<Entry<String, String>> theNamespaces() {
        ServerBatchJobContext serverBatchJobContext = ServerBatchJobContext.THREAD_LOCAL.get()
        serverBatchJobContext.namespacesIterator
    }
}
