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

package com.twcable.grabbit

import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.spring.batch.repository.GrabbitJobExecution
import groovy.transform.CompileStatic
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.explore.JobExplorer

import javax.annotation.Nonnull

/**
 * Represents the status of a client job
 * <p>
 *     This class is used primarily by {@link com.twcable.grabbit.client.servlets.GrabbitJobServlet}, and
 *     {@link com.twcable.grabbit.client.servlets.GrabbitTransactionServlet} for querying job status.
 * </p>
 */
@CompileStatic
class ClientJobStatus {
    final Long jobExecutionId
    final Long transactionID
    final Date startTime
    final Date endTime
    final ExitStatus exitStatus
    final String path
    final Long timeTaken
    final int jcrNodesWritten


    private ClientJobStatus(Long jobExecutionId, Long transactionId, Date startTime, Date endTime,
                            ExitStatus exitStatus, String path, Long timeTaken, int jcrNodesWritten) {
        this.jobExecutionId = jobExecutionId
        this.transactionID = transactionId
        this.startTime = startTime
        this.endTime = endTime
        this.exitStatus = exitStatus
        this.path = path
        this.timeTaken = timeTaken
        this.jcrNodesWritten = jcrNodesWritten
    }

    /**
     * Queries the status of a job.
     * @param explorer The job explorer to query.
     * @param jobExecutionId The job ID to search.
     * @throws {@link NonExistentJobException} If job can not be found.
     * @throws {@link IllegalArgumentException} If explorer is null.
     * @return A {@link ClientJobStatus} for a job's current status. Note, the "timeTaken" parameter will be -1 if the job is still running.
     * In addition, endTime will be null if the job is still running. This method will never return null.
     */
    @Nonnull
    static ClientJobStatus get(@Nonnull final JobExplorer explorer, final long jobExecutionId) throws NonExistentJobException {
        if (explorer == null) throw new IllegalArgumentException("JobExplorer == null")

        final jobExecution = explorer.getJobExecution(jobExecutionId) as GrabbitJobExecution

        if(!jobExecution) {
            throw new NonExistentJobException("Job with execution ID ${jobExecutionId} does not exist")
        }

        long timeTaken = -1
        if (!jobExecution.running) {
            timeTaken = jobExecution.endTime.time - jobExecution.startTime.time
        }
        new ClientJobStatus(
            jobExecution.id,
            jobExecution.transactionID,
            jobExecution.startTime,
            jobExecution.endTime,
            jobExecution.exitStatus,
            jobExecution.jobParameters.getString(ClientBatchJob.PATH),
            timeTaken,
            (jobExecution.stepExecutions?.find { it.stepName == "clientJcrNodes" }?.writeCount ?: -1) as Integer
        )
    }

    /**
     * @param explorer The job explorer to query.
     * @throws IllegalArgumentException If explorer is null
     * @return Get the status of all jobs ever run. Will return an empty collection if no jobs have ever been run.
     */
    @Nonnull
    static Collection<ClientJobStatus> getAll(@Nonnull JobExplorer explorer) {
        if(explorer == null) throw new IllegalArgumentException("JobExplorer == null")

        //Returns all the job instances
        def instances = explorer.getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE)
        if (!instances) return Collections.EMPTY_LIST

        final Collection<JobExecution> executions = instances.collectMany { explorer.getJobExecutions(it) }
        final Collection<Long> jobExecutionIds = executions.collect { it.id }
        jobExecutionIds.collect { Long id ->
            get(explorer, id)
        }
    }

    /**
     * @param explorer The job explorer to query.
     * @param transactionID The transactionID representing the transaction to query.
     * @throws IllegalArgumentException If explorer is null.
     * @return a Collection of {@link ClientJobStatus} representing all jobs as part of a transaction, as denoted by transactionID; or an empty collection if transactionID provided
     * does not map to any known jobs. This may mean that the transaction never existed, or that the data store for the JCR job repository was externally modified
     */
    @Nonnull
    static Collection<ClientJobStatus> getAllForTransaction(@Nonnull final JobExplorer explorer, final long transactionID) {
        if(explorer == null) throw new IllegalArgumentException("jobExplorer == null")
        getAll(explorer).findAll {
            it.transactionID == transactionID
        } as Collection<ClientJobStatus>
    }

    /**
     * @param explorer The job explorer to query.
     * @throws IllegalArgumentException If explorer is null.
     * @return a {@link Collection} representing all transaction ids; or an empty collection if there are no transactions.
     * This may mean that the transactions never existed, or that the data store for the JCR job repository was externally modified
     */
    @Nonnull
    static Collection<String> getAllTransactions(@Nonnull final JobExplorer explorer) {
        if(explorer == null) throw new IllegalArgumentException("jobExplorer == null")
        getAll(explorer).collect {
            it.transactionID
        }.unique() as Collection<String>
    }
}
