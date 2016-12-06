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

package com.twcable.grabbit.client.batch

import com.twcable.grabbit.jcr.JCRUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.sling.jcr.api.SlingRepository
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener

import javax.jcr.Session

/**
 * A JobExecutionListener that hooks into {@link JobExecutionListener#beforeJob(JobExecution)} to setup() the job and
 * {@link JobExecutionListener#afterJob(JobExecution)} to cleanup() after the job
 */
@Slf4j
@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
class ClientBatchJobExecutionListener implements JobExecutionListener {

    /**
     * {@link SlingRepository} is managed by Spring-OSGi
     */
    private SlingRepository slingRepository


    void setSlingRepository(SlingRepository slingRepository) {
        this.slingRepository = slingRepository
    }

    // **********************************************************************
    // METHODS BELOW ARE USED TO SETUP THE CLIENT JOB.
    // THE METHOD beforeJob() IS CALLED AFTER THE CLIENT JOB STARTS AND BEFORE
    // ANY OF THE STEPS ARE EXECUTED
    // **********************************************************************

    /**
     * Callback before a job executes.
     * @param jobExecution the current {@link JobExecution}
     */
    @Override
    void beforeJob(JobExecution jobExecution) {
        log.debug "SlingRepository : ${slingRepository}"
        final clientUsername = jobExecution.jobParameters.getString(ClientBatchJob.CLIENT_USERNAME)
        final Session session = JCRUtil.getSession(slingRepository, clientUsername)

        ClientBatchJobContext.setSession(session)
        log.info "Starting job : ${jobExecution}\n\n"
    }

    // **********************************************************************
    // METHODS BELOW ARE USED TO CLEANUP AFTER CLIENT JOB IS COMPLETE.
    // THE METHOD afterJob() IS CALLED AFTER THE CLIENT JOB STEPS ARE COMPLETE
    // AND BEFORE THE JOB ACTUALLY TERMINATES
    // **********************************************************************

    /**
     * Callback after completion of a job.
     * @param jobExecution the current {@link JobExecution}
     */
    @Override
    void afterJob(JobExecution jobExecution) {
        log.info "Cleanup : ${jobExecution} . Job Complete. Releasing session, and input stream"
        ClientBatchJobContext.cleanup()
        final long timeTaken = jobExecution.endTime.time - jobExecution.startTime.time
        log.info "Grab from ${jobExecution.jobParameters.getString(ClientBatchJob.HOST)} " +
            "for Current Path ${jobExecution.jobParameters.getString(ClientBatchJob.PATH)} took : ${timeTaken} milliseconds\n\n"
    }

}
