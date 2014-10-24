package com.twc.grabbit.server.batch.steps.preprocessor

import com.twc.grabbit.proto.PreProcessorProtos.NamespaceEntry
import groovy.transform.CompileStatic
import org.springframework.batch.item.ItemProcessor

/**
 * A Custom ItemProcessor that effectively acts as a Marshaller for Namespace {prefix->url} mapping.
 */
@CompileStatic
class PreprocessProcessor implements ItemProcessor<Map.Entry<String, String>, NamespaceEntry>{

    /**
     * Converts a Namespace {prefix->url} Entry to a Protocol Buffer Message {@link NamespaceEntry object}
     */
    @Override
    NamespaceEntry process(Map.Entry<String, String> entry) throws Exception {
        NamespaceEntry.Builder namespaceEntryBuilder = NamespaceEntry.newBuilder()
        namespaceEntryBuilder
                .setUri(entry.key)
                .setPrefix(entry.value)
                .build()
    }
}
