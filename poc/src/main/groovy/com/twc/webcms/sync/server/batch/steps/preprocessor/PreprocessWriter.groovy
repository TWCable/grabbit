package com.twc.webcms.sync.server.batch.steps.preprocessor

import com.twc.webcms.sync.proto.PreProcessorProtos.NamespaceEntry
import com.twc.webcms.sync.proto.PreProcessorProtos.NamespaceRegistry
import com.twc.webcms.sync.proto.PreProcessorProtos.Preprocessors
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter

import javax.servlet.ServletOutputStream

/**
 * A Custom ItemWriter that will write the provided Protocol Buffer NamespacesEntries to the {@link PreprocessWriter#servletOutputStream}
 * Will flush the {@link PreprocessWriter#servletOutputStream} after writing provided Protocol Buffer NamespaceEntries
 * @see ItemWriteListener
 */
@Slf4j
@CompileStatic
class PreprocessWriter implements ItemWriter<NamespaceEntry>, ItemWriteListener {

    private ServletOutputStream servletOutputStream

    /**
     * {@link PreprocessWriter#servletOutputStream} must be set before using PreprocessWriter using this method
     * @param the servletOutputStream for current execution
     */
    public void setServletOutputStream(ServletOutputStream servletOutputStream) {
        if(servletOutputStream == null) throw new IllegalArgumentException("servletOutputStream == null")
        this.servletOutputStream = servletOutputStream
    }

    @Override
    void write(List<? extends NamespaceEntry> namespaceEntries) throws Exception {
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
        servletOutputStream.flush()
    }

    @Override
    void onWriteError(Exception exception, List items) {
        log.error "Exception occurred while writing the current chunk", exception
    }

}
