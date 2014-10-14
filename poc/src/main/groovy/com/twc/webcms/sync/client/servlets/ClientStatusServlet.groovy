package com.twc.webcms.sync.client.servlets

import com.twc.webcms.sync.client.batch.ClientBatchJob
import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingSafeMethodsServlet
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.context.ConfigurableApplicationContext

import javax.annotation.Nonnull
import javax.servlet.http.HttpServletResponse

@CompileStatic
@Slf4j
@SlingServlet( methods = ['GET'], paths = ["/bin/twc/client/grab/status"] )
class ClientStatusServlet extends SlingSafeMethodsServlet{

    @Reference(bind='setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        final Collection<String> jobIdsString = request.getParameter("jobIds").split(",").collect()
        final Collection<Long> jobIds = jobIdsString.collect { Long.valueOf(it).longValue() }

        //JobExplorer provides an API to inspect a Job given its jobExecutionId
        JobExplorer jobExplorer = configurableApplicationContext.getBean("clientJobExplorer", JobExplorer)

        final Collection<ClientJobStatus> clientJobsStatus = jobIds.collect { Long id ->
            JobExecution jobExecution = jobExplorer.getJobExecution(id)
            ClientJobStatus.get(jobExecution)
        }

        final String jsonStatusString = new JsonBuilder(clientJobsStatus).toString()
        log.info "Current Status : ${jsonStatusString}"
        response.contentType = "application/json"
        response.status = HttpServletResponse.SC_OK
        response.writer.write(jsonStatusString)
    }

    @CompileStatic
    static class ClientJobStatus {
        final Long jobId
        final Date startTime
        final Date endTime
        final boolean isRunning
        final boolean isStopping
        final ExitStatus exitStatus
        final String path
        final Long timeTaken
        final int jcrNodesWritten

        private ClientJobStatus(Long jobId, Date startTime, Date endTime, boolean isRunning, boolean isStopping,
                                ExitStatus exitStatus, String path, Long timeTaken, int jcrNodesWritten) {
            this.jobId = jobId
            this.startTime = startTime
            this.endTime = endTime
            this.isRunning = isRunning
            this.isStopping = isStopping
            this.exitStatus = exitStatus
            this.path = path
            this.timeTaken = timeTaken
            this.jcrNodesWritten = jcrNodesWritten
        }

        public static ClientJobStatus get(@Nonnull JobExecution jobExecution) {
            if(jobExecution == null) throw new IllegalArgumentException("JobExecution == null")

            long timeTaken = -1
            if(!jobExecution.running) {
                timeTaken = jobExecution.endTime.time - jobExecution.startTime.time
            }
            new ClientJobStatus(
                    jobExecution.jobId,
                    jobExecution.startTime,
                    jobExecution.endTime,
                    jobExecution.running,
                    jobExecution.stopping,
                    jobExecution.exitStatus,
                    jobExecution.jobParameters.getString(ClientBatchJob.PATH),
                    timeTaken,
                    jobExecution.stepExecutions.find { it.stepName == "clientJcrNodes" }.writeCount
            )
        }
    }

}

