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

package com.twcable.grabbit.server.batch.steps.namespace

import com.twcable.grabbit.proto.NamespaceProtos.NamespaceEntry
import groovy.transform.CompileStatic
import org.springframework.batch.item.ItemProcessor

/**
 * A Custom ItemProcessor that effectively acts as a Marshaller for Namespace {prefix->url} mapping.
 */
@CompileStatic
class NamespaceProcessor implements ItemProcessor<Map.Entry<String, String>, NamespaceEntry> {

    /**
     * Converts a Namespace {prefix->url} Entry to a Protocol Buffer Message {@link NamespaceEntry object}
     */
    @Override
    NamespaceEntry process(Map.Entry<String, String> entry) throws Exception {
        NamespaceEntry.Builder namespaceEntryBuilder = NamespaceEntry.newBuilder()
        namespaceEntryBuilder
            .setPrefix(entry.key)
            .setUri(entry.value)
            .build()
    }
}
