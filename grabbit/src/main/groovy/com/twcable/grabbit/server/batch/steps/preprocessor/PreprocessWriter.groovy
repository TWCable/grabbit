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

import com.twcable.grabbit.proto.PreProcessorProtos.NamespaceEntry
import com.twcable.grabbit.proto.PreProcessorProtos.NamespaceRegistry
import com.twcable.grabbit.proto.PreProcessorProtos.Preprocessors
import com.twcable.grabbit.server.batch.ServerBatchJobContext
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter

import javax.servlet.ServletOutputStream

/**
 * A Custom ItemWriter that will write the provided Protocol Buffer NamespacesEntries to the {@link PreprocessWriter#theServletOutputStream()}
 * Will flush the {@link PreprocessWriter#theServletOutputStream()} after writing provided Protocol Buffer NamespaceEntries
 * @see ItemWriteListener
 */
@Slf4j
@CompileStatic
@SuppressWarnings('GrMethodMayBeStatic')
class PreprocessWriter implements ItemWriter<NamespaceEntry>, ItemWriteListener {

    @Override
    void write(List<? extends NamespaceEntry> namespaceEntries) throws Exception {
        ServletOutputStream servletOutputStream = theServletOutputStream()
        if(servletOutputStream == null) throw new IllegalStateException("servletOutputStream must be set.")

        NamespaceRegistry.Builder namespaceRegistryBuilder = NamespaceRegistry.newBuilder()
        namespaceRegistryBuilder.addAllEntry(namespaceEntries)

        Preprocessors preprocessors = Preprocessors.newBuilder()
                .setNamespaceRegistry(namespaceRegistryBuilder.build())
                .build()

        log.debug "Writing Preprocessor Proto message : ${preprocessors}"
        preprocessors.writeDelimitedTo(servletOutputStream)
    }

    @Override
    void beforeWrite(List items) {
        //no-op
    }

    @Override
    void afterWrite(List items) {
        theServletOutputStream().flush()
    }

    @Override
    void onWriteError(Exception exception, List items) {
        log.error "Exception occurred while writing the current chunk", exception
    }

    private ServletOutputStream theServletOutputStream() {
        ServerBatchJobContext serverBatchJobContext = ServerBatchJobContext.THREAD_LOCAL.get()
        serverBatchJobContext.servletOutputStream
    }

}
