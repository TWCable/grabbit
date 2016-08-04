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

package com.twcable.grabbit.client.batch.steps.namespace

import com.twcable.grabbit.client.batch.ClientBatchJobContext
import com.twcable.grabbit.proto.NamespaceProtos.NamespaceEntry
import com.twcable.grabbit.proto.NamespaceProtos.NamespaceRegistry
import groovy.transform.CompileStatic
import groovy.transform.WithWriteLock
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
 * A Custom Tasklet that will read a {@link NamespaceRegistry} object from {@link NamespaceSyncTasklet#theInputStream()}  and it to the {@link NamespaceSyncTasklet#theSession()}
 *  See client-batch-job.xml for how this is used.
 */
@Slf4j
@CompileStatic
@SuppressWarnings('GrMethodMayBeStatic')
class NamespaceSyncTasklet implements Tasklet {

    /**
     * This tasklet can be potentially executed by any number of threads, with possible
     * contention on the shared JCR NamespaceRegistry.
     * Adding a WriteLock on this method ensures only one thread can use this method at
     * a time, thus protecting the NamespaceRegistry.
     */
    @WithWriteLock
    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info "Start writing namespaces."
        NamespaceRegistry namespaceRegistry = NamespaceRegistry.parseDelimitedFrom(theInputStream())

        if (!namespaceRegistry) {
            log.warn "No namespaces received from server"
            log.info "Finished writing namespaces."
            return RepeatStatus.FINISHED
        }

        log.trace "Received namespaces : ${namespaceRegistry}"

        writeToJcr(namespaceRegistry, theSession())
        theSession().save()
        log.info "Finished writing namespaces."
        return RepeatStatus.FINISHED
    }


    private InputStream theInputStream() {
       ClientBatchJobContext.inputStream
    }


    private Session theSession() {
        ClientBatchJobContext.session
    }


    private static void writeToJcr(NamespaceRegistry namespaceRegistry, Session session) {
        try {
            final NamespaceHelper namespaceHelper = new NamespaceHelper(session)
            namespaceRegistry.entryList.each { NamespaceEntry namespaceEntry ->
                try {
                    namespaceHelper.registerNamespace(namespaceEntry.prefix, namespaceEntry.uri)
                }
                catch (NamespaceException e) {
                    if (e.message.contains("mapping already exists")) {
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
            log.error "Exception while unmarshalling Namespaces: ${namespaceRegistry}", e
        }
    }
}
