package com.twc.grabbit.client.batch.steps.preprocessor

import com.twc.grabbit.client.batch.ClientBatchJobContext
import com.twc.grabbit.proto.PreProcessorProtos
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.NamespaceHelper
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

import javax.jcr.NamespaceException
import javax.jcr.RepositoryException
import javax.jcr.Session

/**
  * A Custom Tasklet that will read a {@link PreProcessorProtos.Preprocessors} object from {@link PreprocessTasklet#theInputStream()}  and it to the {@link PreprocessTasklet#theSession()}
 *  See client-batch-job.xml for how this is used.
 */
@Slf4j
@CompileStatic
@SuppressWarnings('GrMethodMayBeStatic')
class PreprocessTasklet implements Tasklet {

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        PreProcessorProtos.Preprocessors preprocessors = PreProcessorProtos.Preprocessors.parseDelimitedFrom(theInputStream())
        log.debug "Received Preprocessor : ${preprocessors}"

        writeToJcr(preprocessors, theSession())
        theSession().save()
        return RepeatStatus.FINISHED
    }

    private InputStream theInputStream() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.inputStream
    }

    private Session theSession() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.session
    }

    private static void writeToJcr(PreProcessorProtos.Preprocessors preprocessorsProto, Session session) {
        try {
            log.debug "Received Preprocessors Proto: ${preprocessorsProto}"
            final NamespaceHelper namespaceHelper = new NamespaceHelper(session)
            PreProcessorProtos.NamespaceRegistry namespaceRegistryProto = preprocessorsProto.namespaceRegistry
            namespaceRegistryProto.entryList.each { PreProcessorProtos.NamespaceEntry namespaceEntry ->
                try {
                    namespaceHelper.registerNamespace(namespaceEntry.prefix, namespaceEntry.uri)
                }
                catch(NamespaceException e) {
                    if(e.message.contains("mapping already exists")) {
                        log.warn "Mapping for Nameapace prefix : ${namespaceEntry.prefix} already exists. Received NamespaceEntry mapping : ${namespaceEntry}"
                        return
                    }
                    else {
                        throw e
                    }
                }
            }
            session.save()
        }
        catch (RepositoryException e) {
            log.error "Exception while unmarshalling Preprocessors: ${preprocessorsProto}", e
        }
    }
}
