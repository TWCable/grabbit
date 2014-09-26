package com.twc.webcms.sync.server.batch.steps.preprocessor

import groovy.transform.CompileStatic
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.NonTransientResourceException
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.UnexpectedInputException

import static java.util.Map.Entry

/**
 * A Custom ItemReader that provides the "next" Namespace Entry from the {@link PreprocessReader#namespaces}.
 * Returns null to indicate that all Items have been read.
 */
@CompileStatic
class PreprocessReader implements ItemReader<Entry<String, String>> {

    private Iterator<Entry<String, String>> namespaces

    /**
     * {@link PreprocessReader#namespaces} must be set before using PreprocessReader using this method
     * @param the namespaces for current execution
     */
    public void setNamespaces(Iterator<Entry<String, String>> namespaces) {
        if(namespaces == null) throw new IllegalArgumentException("namespaces == null")
        this.namespaces = namespaces
    }

    @Override
    Entry<String, String> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if(namespaces == null) throw new IllegalStateException("namespaces must be set.")

        if(namespaces.hasNext()) {
            namespaces.next()
        }
        else {
            null
        }
    }
}
