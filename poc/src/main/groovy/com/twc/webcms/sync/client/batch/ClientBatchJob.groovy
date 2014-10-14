package com.twc.webcms.sync.client.batch

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.launch.JobOperator
import org.springframework.context.ConfigurableApplicationContext

import javax.annotation.Nonnull

/**
 * A simple helper class that given a Application Context and initial configuration conditions, will
 * return a ClientBatchJob instance with a valid {@link JobOperator}
 */
@Slf4j
@CompileStatic
class ClientBatchJob {

    public static final String PATH = "path"
    public static final String HOST = "host"
    public static final String PORT = "port"
    public static final String USERNAME = "username"
    public static final String PASSWORD = "password"

    private final Map<String,String> jobParameters
    private final JobOperator jobOperator

    protected ClientBatchJob(@Nonnull Map<String, String> jobParameters, @Nonnull JobOperator jobOperator) {
        if(jobParameters == null) throw new IllegalArgumentException("JobParameters == null")
        if(jobOperator == null) throw new IllegalArgumentException("jobOperator == null")

        this.jobParameters = jobParameters
        this.jobOperator = jobOperator
    }

    /**
     * Method to be called to start a job for given specific parameters
     * @return ID of the current Job's JobExecution instance
     */
    public Long start() {
        final String JOB_NAME = "clientJob"
        final String jobParametersString = jobParameters.collect { String key, String value ->
            "${key}=${value}"
        }.join(",")
        log.debug "Current Job Params : ${jobParametersString}"
        final Long jobExecutionId = jobOperator.start(JOB_NAME, jobParametersString)
        log.info "Kicked off job with ID : ${jobExecutionId}"
        return jobExecutionId
    }

    // **********************************************************************
    // INNER CLASSES
    // **********************************************************************

    @CompileStatic
    static class ServerBuilder {
        final ConfigurableApplicationContext configAppContext
        String host
        String port

        ServerBuilder(ConfigurableApplicationContext configurableApplicationContext) {
            this.configAppContext = configurableApplicationContext
        }

        CredentialsBuilder andServer(String host, String port) {
            this.host = host
            this.port = port
            return new CredentialsBuilder(this)
        }
    }

    @CompileStatic
    static class CredentialsBuilder {
        final ServerBuilder parentBuilder
        String username
        String password

        CredentialsBuilder(ServerBuilder parentBuilder) {
            this.parentBuilder = parentBuilder
        }

        PathBuilder andCredentials(String username, String password) {
            this.username = username
            this.password = password
            return new PathBuilder(this)
        }
    }

    @CompileStatic
    static class PathBuilder {
        final CredentialsBuilder parentBuilder
        String path

        PathBuilder(CredentialsBuilder parentBuilder) {
            this.parentBuilder = parentBuilder
        }

        Builder andPath(String path){
            this.path = path
            return new Builder(this)
        }
    }

    @CompileStatic
    static class Builder {
        final PathBuilder pathBuilder
        final CredentialsBuilder credentialsBuilder
        final ServerBuilder serverBuilder

        Builder(PathBuilder parentBuilder) {
            this.pathBuilder = parentBuilder
            this.credentialsBuilder = pathBuilder.parentBuilder
            this.serverBuilder = credentialsBuilder.parentBuilder
        }

        ClientBatchJob build() {
            return new ClientBatchJob(
                [
                        "timestamp": System.currentTimeMillis() as String,
                        "${PATH}" : pathBuilder.path,
                        "${HOST}" : serverBuilder.host,
                        "${PORT}" : serverBuilder.port,
                        "${USERNAME}" : credentialsBuilder.username,
                        "${PASSWORD}" : credentialsBuilder.password

                ] as Map<String, String>,
                serverBuilder.configAppContext.getBean("clientJobOperator", JobOperator)
            )
        }
    }

}
