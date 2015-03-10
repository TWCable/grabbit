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

package com.twcable.grabbit.client.batch.steps.jcrnodes

import com.twcable.grabbit.client.batch.ClientBatchJob
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener

/**
 * A Custom {@link StepExecutionListener}
 */
@Slf4j
@CompileStatic
class JcrNodesStepExecutionListener implements StepExecutionListener{

    @Override
    void beforeStep(StepExecution stepExecution) {
        log.info "Starting JcrNodes Step ${stepExecution}\n"
    }

    /**
     * Assumes that Step Completed successfully
     * Logs information about the Step like the currentPath and the {@link StepExecution#writeCount}
     * @param stepExecution current StepExecution
     * @return ExitStatus.COMPLETED (Assumes Step Completed and there were no errors)
     */
    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.jobParameters
        final String currentPath = jobParameters.getString(ClientBatchJob.PATH)
        log.info "JcrNodes Step Complete. Current Path : ${currentPath} . Total nodes written : ${stepExecution.writeCount}\n\n"
        return ExitStatus.COMPLETED
    }
}
