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
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Deactivate
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
@CompileStatic
@Component(label = "Workflow Manager", description = "Workflow Manager", immediate = true, metatype = true, enabled = true)
@Service(WorkflowManager)
@SuppressWarnings('GroovyUnusedDeclaration')
class DefaultWorkFlowManager implements WorkflowManager {

    @Reference(bind = 'setWorkflowLauncher')
    WorkflowLauncher workflowLauncher

    private ConcurrentHashMap<String, AtomicInteger> launcherConfigs


    @Activate
    void activate() {
        launcherConfigs = new ConcurrentHashMap<String, AtomicInteger>()
        log.debug "Activate : LauncherConfigs Map : ${launcherConfigs}"
    }


    @Deactivate
    void deactivate() {
        launcherConfigs = null
    }


    @Override
    void turnOff(Collection<String> wfConfigIds) {
        wfConfigIds.each { String id ->
            log.debug "Current ID: ${id} and Map: ${launcherConfigs}"
            def val = launcherConfigs.putIfAbsent(id, new AtomicInteger(1))
            if (val) {
                //if val != null, that means id is already processed
                log.info "Current ID : ${id} is already processed. LauncherConfigs Map is : ${launcherConfigs}"
                AtomicInteger count = launcherConfigs.get(id)
                count.getAndIncrement()
                launcherConfigs.putIfAbsent(id, count)
            }
            else {
                //if val = null, that means this is the first time id is requested
                //Put it in launcherConfigs and process that id
                log.debug "Putting id: ${id} in Map"
                launcherConfigs.putIfAbsent(id, new AtomicInteger(1))
                log.info "Turning off config : ${id}"
                processConfig(id, LauncherState.OFF)
            }
        }
    }


    @Override
    void turnOn(Collection<String> wfConfigIds) {

        //If there is nothing in the launcherConfigs,
        //there is something wrong. Return false
        if (launcherConfigs.isEmpty()) throw new IllegalStateException("Launcher Configs cannot be empty")

        //At this point, requested wfConfigIds should be already present in the launcherConfigs
        //Decrement counts of all the requested wfConfigIds
        wfConfigIds.each { String id ->
            AtomicInteger count = launcherConfigs.get(id)
            if (!count) throw new IllegalStateException("launcherConfig Map value for ${id} cannot be null")
            count.decrementAndGet()
            launcherConfigs.put(id, count)
        }

        log.debug "LauncherConfig in turnOn() is : ${launcherConfigs}"

        launcherConfigs.each { id, count ->
            if (count.get() < 1) {
                //if a count is < 0, its ready to turn that id back on
                log.info "Turning on configId : ${id}"
                processConfig(id, LauncherState.ON)
            }
        }

        //Reset the map iff all the configs are turned on
        reset()
    }

    /**
     * Turns Off or Turns On config for the given id
     * @param id the workflow Id
     * @param state representing what is to be done with the Id.
     *
     * @see LauncherState
     */
    private void processConfig(String id, LauncherState state) {
        final ConfigEntry currentEntry = workflowLauncher.configEntries.find { it.id == id }
        if (state == LauncherState.OFF && !currentEntry.enabled) {
            log.info "Current Workflow Launcher : ${id} is already Turned Off. No-op"
        }
        else if (state == LauncherState.ON && currentEntry.enabled) {
            log.info "Current Workflow Launcher : ${id} is already Turned On. No-op"
        }
        else {
            final ConfigEntry updatedEntry = new ConfigEntry(
                currentEntry.eventType,
                currentEntry.glob,
                currentEntry.nodetype,
                currentEntry.whereClause,
                currentEntry.workflow,
                currentEntry.id,
                currentEntry.description,
                //Toggle enabled value.
                !currentEntry.enabled,
                currentEntry.excludeList,
                currentEntry.runModes
            )
            log.info "Editing config for id:  ${id}"
            workflowLauncher.editConfigEntry(id, updatedEntry)
        }
    }

    /**
     * Resets {@link #launcherConfigs} if all of the counts in it are < 1
     */
    private void reset() {
        final doneCount = launcherConfigs.count { id, count -> count.get() < 1 }

        if (doneCount == launcherConfigs.size()) {
            //All configs are processed. We can clear them now
            //TODO: This will probably fail if 2nd Grabbit request comes in before the 1st Grabbit request is completed
            log.info "(Re)initializing LauncherConfigs Map : ${launcherConfigs}"
            launcherConfigs = new ConcurrentHashMap<String, AtomicInteger>()
        }
    }

    /**
     * Simple enum representing Workflow Launcher OFF state or ON state
     * {@link LauncherState#OFF} means it needs to be turned off
     * {@link LauncherState#OFF} means it needs to be turned on
     */
    enum LauncherState {
        ON, OFF
    }
}
