package com.twc.webcms.sync.client.batch.steps.preprocessor

import com.twc.webcms.sync.client.batch.ClientBatchJobContext
import com.twc.webcms.sync.proto.PreProcessorProtos
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.NamespaceHelper
import org.springframework.batch.core.ItemWriteListener
import org.springframework.batch.item.ItemWriter

import javax.jcr.RepositoryException
import javax.jcr.Session

/**
 * A Custom ItemWriter that will write the provided {@link PreProcessorProtos.Preprocessors} to the {@link PreprocessWriter#session}
 * Will save() the {@link PreprocessWriter#session} after writing provided Protocol Buffer NamespaceEntries
 * @see ItemWriteListener
 */
@Slf4j
@CompileStatic
class PreprocessWriter implements ItemWriter<PreProcessorProtos.Preprocessors>, ItemWriteListener {

    private Session session

    @Override
    void beforeWrite(List items) {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        this.session = clientBatchJobContext.session
    }

    @Override
    void afterWrite(List items) {
        log.debug "Done with Preprocess Step"
        session.save()

    }

    @Override
    void onWriteError(Exception exception, List items) {
        log.error "Exception while writing the JCR Namespaces to the current session: ${session}. ", exception
    }

    @Override
    void write(List<? extends PreProcessorProtos.Preprocessors> preprocessorses) throws Exception {
        for(PreProcessorProtos.Preprocessors preprocessor : preprocessorses) {
            writeToJcr(preprocessor, session)
        }
    }

    private static void writeToJcr(PreProcessorProtos.Preprocessors preprocessorsProto, Session session) {
        try {
            log.debug "Received Preprocessors Proto: ${preprocessorsProto}"
            final NamespaceHelper namespaceHelper = new NamespaceHelper(session)
            PreProcessorProtos.NamespaceRegistry namespaceRegistryProto = preprocessorsProto.namespaceRegistry
            namespaceRegistryProto.entryList.each { PreProcessorProtos.NamespaceEntry namespaceEntry ->
                namespaceHelper.registerNamespace(namespaceEntry.prefix, namespaceEntry.uri)
            }
            session.save()
        }
        catch (RepositoryException e) {
            log.error "Exception while unmarshalling Preprocessors: ${preprocessorsProto}", e
        }
    }
}
