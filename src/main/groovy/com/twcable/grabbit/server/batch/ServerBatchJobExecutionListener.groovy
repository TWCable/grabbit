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

package com.twcable.grabbit.server.batch

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener

/**
 * A JobExecutionListener that hooks into {@link JobExecutionListener#beforeJob(JobExecution)} and
 * {@link JobExecutionListener#afterJob(JobExecution)}
 */
@Slf4j
@CompileStatic
class ServerBatchJobExecutionListener implements JobExecutionListener {

    /**
     * Callback before a job executes.
     * @param jobExecution the current {@link JobExecution}
     */
    @Override
    void beforeJob(JobExecution jobExecution) {
        log.info "Starting job : ${jobExecution}\n\n"
    }

    /**
     * Callback after completion of a job.
     * Cleans up current Thread's ThreadLocal from {@link ServerBatchJobContext#THREAD_LOCAL}
     * @param jobExecution the current {@link JobExecution}
     */
    @Override
    void afterJob(JobExecution jobExecution) {
        log.info "Clearing ThreadLocal for current job: ${jobExecution} . Job Complete"
        ServerBatchJobContext serverBatchJobContext = ServerBatchJobContext.THREAD_LOCAL.get()
        try {
            serverBatchJobContext.servletOutputStream.close()
        }
        catch (Exception ignore) { /* just doing cleanup */
        }
        ServerBatchJobContext.THREAD_LOCAL.remove()
        final long timeTaken = jobExecution.endTime.time - jobExecution.startTime.time
        log.info "Content sent for ${jobExecution.jobParameters.getString(ServerBatchJob.PATH)} took : ${timeTaken} milliseconds\n\n"
    }
}
