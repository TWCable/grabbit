package com.twcable.grabbit.client.batch.steps.workspace

import com.twcable.grabbit.client.batch.ClientBatchJob
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.job.flow.FlowExecutionStatus
import org.springframework.batch.core.job.flow.JobExecutionDecider

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

/**
 * This class decides whether to execute the @see{@link DeleteBeforeWriteTasklet} based on the
 * deleteBeforeWrite parameter in the job parameters
 */
@CompileStatic
@Slf4j
class DeleteBeforeWriteDecider implements JobExecutionDecider {

    /**
     * Strategy for branching an execution based on the state of an ongoing
     * {@link JobExecution}. The return value will be used as a status to
     * determine the next step in the job.
     *
     * @param jobExecution a job execution
     * @param stepExecution the latest step execution (may be null)
     * @return the exit status code
     */
    @Override
    FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        final boolean shouldDeleteBeforeWrite = jobExecution.jobParameters.getString(ClientBatchJob.DELETE_BEFORE_WRITE).toBoolean()
        log.info shouldDeleteBeforeWrite ? "Will delete nodes under job path before writing for ${jobExecution.id}..." :
                                            "Will retain nodes under job path for ${jobExecution.id}..."
        return shouldDeleteBeforeWrite ? new FlowExecutionStatus("YES") : new FlowExecutionStatus("NO")
    }
}
