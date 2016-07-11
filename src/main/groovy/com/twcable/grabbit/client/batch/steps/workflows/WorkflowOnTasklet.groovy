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

package com.twcable.grabbit.client.batch.steps.workflows

import com.twcable.grabbit.client.batch.workflows.WorkflowManager
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

@CompileStatic
@Slf4j
class WorkflowOnTasklet implements Tasklet {

    private WorkflowManager workflowManager

    private String workflowConfigs


    void setWorkflowManager(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager
    }


    void setWorkflowConfigs(String workflowConfigs) {
        log.info("WorkflowConfig : ${workflowConfigs}")
        this.workflowConfigs = workflowConfigs
    }


    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!workflowConfigs || !workflowConfigs.contains("/etc/workflow") /* temporary for testing */) {
            //nothing to process as there are no workflow configs for the current path
            log.info "Nothing to process..."
            return RepeatStatus.FINISHED
        }

        Collection<String> configIds = workflowConfigs.split("\\|") as Collection<String>

        workflowManager.turnOn(configIds)
        return RepeatStatus.FINISHED
    }
}
