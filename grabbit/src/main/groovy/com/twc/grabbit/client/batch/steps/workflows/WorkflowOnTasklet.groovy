package com.twc.grabbit.client.batch.steps.workflows

import com.twc.grabbit.client.batch.workflows.WorkflowManager
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
        if(!workflowConfigs || !workflowConfigs.contains("/etc/workflow") /* temporary for testing */ ) {
            //nothing to process as there are no workflow configs for the current path
            log.info "Nothing to process..."
            return RepeatStatus.FINISHED
        }

        Collection<String> configIds = workflowConfigs.split("\\|") as Collection<String>

        workflowManager.turnOn(configIds)
        return RepeatStatus.FINISHED
    }
}
