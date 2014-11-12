package com.twc.grabbit

import com.twc.grabbit.client.batch.ClientBatchJob
import groovy.transform.CompileStatic
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.explore.JobExplorer

import javax.annotation.Nonnull

@CompileStatic
class ClientJobStatus {
    final Long jobId
    final Date startTime
    final Date endTime
    final ExitStatus exitStatus
    final String path
    final Long timeTaken
    final int jcrNodesWritten

    private ClientJobStatus(Long jobId, Date startTime, Date endTime,
                            ExitStatus exitStatus, String path, Long timeTaken, int jcrNodesWritten) {
        this.jobId = jobId
        this.startTime = startTime
        this.endTime = endTime
        this.exitStatus = exitStatus
        this.path = path
        this.timeTaken = timeTaken
        this.jcrNodesWritten = jcrNodesWritten
    }

    public static ClientJobStatus get(@Nonnull JobExplorer explorer, @Nonnull Long jobId) {
        if(explorer == null) throw new IllegalArgumentException("JobExplorer == null")
        if(jobId == null) throw new IllegalArgumentException("JobId == null")

        final JobExecution jobExecution = explorer.getJobExecution(jobId)

        long timeTaken = -1
        if(!jobExecution.running) {
            timeTaken = jobExecution.endTime.time - jobExecution.startTime.time
        }
        new ClientJobStatus(
                jobExecution.jobId,
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
