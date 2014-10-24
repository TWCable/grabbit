package com.twc.grabbit.server.batch.steps.preprocessor

import com.twc.grabbit.proto.PreProcessorProtos.NamespaceEntry
import com.twc.grabbit.proto.PreProcessorProtos.NamespaceRegistry
import com.twc.grabbit.proto.PreProcessorProtos.Preprocessors
import com.twc.grabbit.server.batch.ServerBatchJobContext
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
