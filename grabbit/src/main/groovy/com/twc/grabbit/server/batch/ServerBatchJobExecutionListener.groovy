package com.twc.grabbit.server.batch

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
class ServerBatchJobExecutionListener implements JobExecutionListener{

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
        try { serverBatchJobContext.servletOutputStream.close() } catch (Exception ignore) { /* just doing cleanup */ }
        ServerBatchJobContext.THREAD_LOCAL.remove()
        final long timeTaken = jobExecution.endTime.time - jobExecution.startTime.time
        log.info "Content sent for ${jobExecution.jobParameters.getString(ServerBatchJob.PATH)} took : ${timeTaken} milliseconds\n\n"
    }
}
