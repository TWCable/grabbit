package com.twc.grabbit.client.batch.steps.jcrnodes

import com.twc.grabbit.client.batch.ClientBatchJob
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
