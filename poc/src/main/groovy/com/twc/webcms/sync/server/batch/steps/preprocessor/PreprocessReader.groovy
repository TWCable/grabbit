package com.twc.webcms.sync.server.batch.steps.preprocessor

import com.twc.webcms.sync.server.batch.ServerBatchJobContext
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
