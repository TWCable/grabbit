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

package com.twcable.grabbit.client.batch.workflows

import com.day.cq.workflow.launcher.ConfigEntry
import com.day.cq.workflow.launcher.WorkflowLauncher
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.jcr.RepositoryException

/**
 * A simple Stubbed {@link WorkflowLauncher} implementation to simplify
 * tracking and verification when testing {@link WorkflowManager}
 */
@Slf4j
@CompileStatic
class StubWorkflowLauncher implements WorkflowLauncher {

    private int editConfigCallCount

    private List<ConfigEntry> configEntries


    public StubWorkflowLauncher(int noOfEntries) {
        editConfigCallCount = 0
        configEntries = (1..noOfEntries).collect { i ->
            new ConfigEntry(0, "", "", "", "", "id${i}", "", true, [], [])
        }
    }


    @Override
    void addConfigEntry(ConfigEntry configEntry) throws RepositoryException {
        log.info "Adding configEntry : ${configEntry}"
    }


    @Override
    void removeConfigEntry(String s) throws RepositoryException {
        log.info "Removing configEntry with ID : ${s}"
    }


    @Override
    List<ConfigEntry> getConfigEntries() {
        configEntries
    }


    @Override
    void editConfigEntry(String s, ConfigEntry configEntry) throws RepositoryException {
        log.info "Editing configEntry with ID : ${s} with new entry: ${configEntry}"
        configEntries.find { entry -> entry.id == configEntry.id }.enabled = configEntry.enabled
        editConfigCallCount++
    }
}
