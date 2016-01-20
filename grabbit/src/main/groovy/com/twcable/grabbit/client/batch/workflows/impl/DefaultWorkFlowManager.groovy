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

package com.twcable.grabbit.client.batch.workflows.impl

import com.day.cq.workflow.launcher.ConfigEntry
import com.day.cq.workflow.launcher.WorkflowLauncher
import com.twcable.grabbit.client.batch.workflows.WorkflowManager
import groovy.transform.CompileStatic
import groovy.transform.WithWriteLock
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Deactivate
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service

import javax.annotation.Nonnull
import java.lang.String as WorkflowID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger as JobUseCount

@Slf4j
@CompileStatic
@Component(label = "Workflow Manager", description = "Workflow Manager", immediate = true, metatype = true, enabled = true)
@Service(WorkflowManager)
@SuppressWarnings('GroovyUnusedDeclaration')
class DefaultWorkFlowManager implements WorkflowManager {

    @Reference(bind = 'setWorkflowLauncher')
    WorkflowLauncher workflowLauncher

    private ConcurrentHashMap<WorkflowID, JobUseCount> workflowSemaphore


    @Activate
    void activate() {
        workflowSemaphore = new ConcurrentHashMap<WorkflowID, JobUseCount>()
        log.debug "Activate : Workflow Semaphore : ${workflowSemaphore}"
    }


    @Deactivate
    void deactivate() {
        workflowSemaphore = null
    }


    @Override
    void turnOff(@Nonnull final Collection<WorkflowID> workflowConfigIds) {
        workflowConfigIds.each { WorkflowID id ->
            log.debug "Turn off ID: ${id} for thread ${Thread.currentThread().id} and Map: ${workflowSemaphore}"
            //Initializes the key if this particular workflow hasn't been turned off before
            workflowSemaphore.putIfAbsent(id, new JobUseCount(0))
            if (workflowSemaphore.get(id).incrementAndGet() == 1) {
                //If this is the first time the workflow has been turned off, be sure to update the configuration
                updateConfig(id, false)
            }
        }
    }


    @Override
    void turnOn(Collection<WorkflowID> workflowConfigIds) {
        workflowConfigIds.each { WorkflowID id ->
            log.debug "Turn on ID: ${id} for thread ${Thread.currentThread().id} and Map: ${workflowSemaphore}"
            final launcherConfig = workflowSemaphore.get(id)
            if(!launcherConfig) {
                log.error "Was requested to turn on workflow with id '${id}', but workflow was never turned off, or no such workflow could be found"
                return
            }
            if(launcherConfig.decrementAndGet() == 0) {
                updateConfig(id, true)
            }
        }
    }

    /**
     * Updates a workflow config by enabling or disabling it
     * @param configId the workflow configuration ID
     * @param enable should the configuration be enabled?
     */
    private void updateConfig(WorkflowID configId, boolean enable) {
        final ConfigEntry configEntry = workflowLauncher.configEntries.find { it.id == configId }
        if (configEntry == null) {
            log.error "Was expecting a config entry for ${configId}, but none was found!"
            return
        }
        if (configEntry.enabled == enable) {
            log.info "Was expecting to change status of ${configId} to enabled=${enable}, but config entry was already of status enabled=${configEntry.enabled}"
            return
        }
        log.info "Updating workflow configuration for id '${configId}' to enabled=${enable}"
        configEntry.setEnabled(enable)
        updateConfig(configId, configEntry)
    }

    /**
     * Reentrant wrapper for Workflow Launcher to update config entries. This is because AEM provides no mechanism in
     * workflowLauncher.editConfigEntry to guarantee order of config saves - which we obviously need in this context.
     * The above code ensures that updateConfig is called in the correct order, but it can't guarantee that the session
     * is saved in the same order
     * @param configId
     * @param configEntry
     */
    @WithWriteLock
    private void updateConfig(final WorkflowID configId, final ConfigEntry configEntry) {
        workflowLauncher.editConfigEntry(configId, configEntry)
    }
}
