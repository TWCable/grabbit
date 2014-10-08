package com.twc.webcms.sync.client.batch

import com.twc.webcms.sync.jcr.JcrUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.sling.jcr.api.SlingRepository
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.batch.core.JobParameters

import javax.jcr.Session

/**
 * A JobExecutionListener that hooks into {@link JobExecutionListener#beforeJob(JobExecution)} to setup() the job and
 * {@link JobExecutionListener#afterJob(JobExecution)} to cleanup() after the job
 */
@Slf4j
@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
class ClientBatchJobExecutionListener implements JobExecutionListener{

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
        setup(jobExecution.jobParameters)
        log.info "Starting job : ${jobExecution}\n\n"
    }

    /**
     * Method that makes request to provided server using {@link JobParameters}
     * Creates a {@link Session} with using the {@link ClientBatchJobExecutionListener#slingRepository}
     * Stores the InputStream and Session on Current Thread's {@link ThreadLocal}
     * @param jobParameters
     */
    private void setup(final JobParameters jobParameters) {
        log.debug "SlingRepository : ${slingRepository}"
        HttpResponse response = doRequest(jobParameters)
        HttpEntity responseEntity = response.entity
        final InputStream inputStream = responseEntity.content

        final Session session = JcrUtil.getSession(slingRepository, "admin")

        ClientBatchJobContext clientBatchJobContext = new ClientBatchJobContext(inputStream, session)
        ClientBatchJobContext.THREAD_LOCAL.set(clientBatchJobContext)

    }

    /**
     * Gets a Http Get connection for the provided authentication information
     * @param username
     * @param password
     * @return a {@link DefaultHttpClient} instance
     */
    private DefaultHttpClient getHttpClient(final String username, final String password) {
        DefaultHttpClient client = new DefaultHttpClient()

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider()
        credentialsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password)
        )
        client.setCredentialsProvider(credentialsProvider)
        client
    }

    /**
     * Makes a Http Get request to the grab path and returns the response
     * @param jobParameters that contain information like the path, host, port, etc.
     * @return the httpResponse
     */
    private HttpResponse doRequest(JobParameters jobParameters) {
        final String path = jobParameters.getString(ClientBatchJob.PATH)
        final String host = jobParameters.getString(ClientBatchJob.HOST)
        final String port = jobParameters.getString(ClientBatchJob.PORT)
        final String username = jobParameters.getString(ClientBatchJob.USERNAME)
        final String password = jobParameters.getString(ClientBatchJob.PASSWORD)

        final String grabPath = "http://${host}:${port}/bin/twc/server/grab?path=${path}"

        //create the get request
        HttpGet get = new HttpGet(grabPath)
        HttpResponse response = getHttpClient(username, password).execute(get)
        response
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
        log.info "Cleanup : ${jobExecution} . Job Complete. Clearing THREAD_LOCAL. Releasing inputStream, session"
        cleanup()
        final long timeTaken = jobExecution.endTime.time - jobExecution.startTime.time
        log.info "Grab from ${jobExecution.jobParameters.getString(ClientBatchJob.HOST)} " +
                "for Current Path ${jobExecution.jobParameters.getString(ClientBatchJob.PATH)} took : ${timeTaken} milliseconds\n\n"
    }

    /**
     * Cleans up the current Thread's {@link ThreadLocal}
     */
    private void cleanup() {
        ClientBatchJobContext clientBatchJobContext = ClientBatchJobContext.THREAD_LOCAL.get()
        try { clientBatchJobContext.inputStream.close() } catch (Exception ignore) { /* just doing cleanup */ }
        try { clientBatchJobContext.session.logout() } catch (Exception ignore) { /* just doing cleanup */ }
        ClientBatchJobContext.THREAD_LOCAL.remove()
    }

}
