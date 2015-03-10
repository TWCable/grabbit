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
import groovy.transform.CompileStatic
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.explore.JobExplorer

import javax.annotation.Nonnull

@CompileStatic
class ClientJobStatus {
    final Long jobExecutionId
    final Date startTime
    final Date endTime
    final ExitStatus exitStatus
    final String path
    final Long timeTaken
    final int jcrNodesWritten

    private ClientJobStatus(Long jobExecutionId, Date startTime, Date endTime,
                            ExitStatus exitStatus, String path, Long timeTaken, int jcrNodesWritten) {
        this.jobExecutionId = jobExecutionId
        this.startTime = startTime
        this.endTime = endTime
        this.exitStatus = exitStatus
        this.path = path
        this.timeTaken = timeTaken
        this.jcrNodesWritten = jcrNodesWritten
    }

    public static ClientJobStatus get(@Nonnull JobExplorer explorer, @Nonnull Long jobExecutionId) {
        if(explorer == null) throw new IllegalArgumentException("JobExplorer == null")
        if(jobExecutionId == null) throw new IllegalArgumentException("JobExecutionId == null")

        final jobExecution = explorer.getJobExecution(jobExecutionId)

        long timeTaken = -1
        if(!jobExecution.running) {
            timeTaken = jobExecution.endTime.time - jobExecution.startTime.time
        }
        new ClientJobStatus(
                jobExecution.id,
                jobExecution.startTime,
                jobExecution.endTime,
                jobExecution.exitStatus,
                jobExecution.jobParameters.getString(ClientBatchJob.PATH),
                timeTaken,
                jobExecution.stepExecutions?.find { it.stepName == "clientJcrNodes" }?.writeCount ?: -1
        )
    }

    public static List<ClientJobStatus> getAll(@Nonnull JobExplorer explorer) {
        if(explorer == null) throw new IllegalArgumentException("JobExplorer == null")
        
        //Returns all the job instances that are currently running
        def instances = explorer.getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE)
        if(!instances) return Collections.EMPTY_LIST

        final List<JobExecution> executions = instances.collectMany { explorer.getJobExecutions(it) }
        final List<Long> jobExecutionIds = executions.collect { it.id }
        jobExecutionIds.collect { Long id ->
            get(explorer, id)
        }
    }
}
