package com.twc.webcms.sync.client.batch.steps.preprocessor

import com.twc.webcms.sync.client.batch.ClientBatchJobContext
import com.twc.webcms.sync.proto.PreProcessorProtos
import groovy.transform.CompileStatic
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException

/**
 * A Custom ItemReader that provides the "next" Namespace Entry from the {@link ClientBatchJobContext#inputStream}.
 * Returns null to indicate that all Items have been read.
 */
@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
class PreprocessReader implements ItemReader<PreProcessorProtos.Preprocessors> {

    private boolean doneReading

    public PreprocessReader() {
        this.doneReading = false
    }

    @Override
    PreProcessorProtos.Preprocessors read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        // We want to make sure that this read() method is called only once. because server sends back exactly 1 message
        // with PreprocessorProtos.Preprocessors message (which contains the namespaces information)
        if(doneReading) {
            doneReading = false
            return null
        }
        doneReading = true
        PreProcessorProtos.Preprocessors preprocessors = PreProcessorProtos.Preprocessors.parseDelimitedFrom(theInputStream())
        return preprocessors
    }

    private InputStream theInputStream() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        clientBatchJobContext.inputStream
    }
}
