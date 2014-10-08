package com.twc.webcms.sync.client.batch

import groovy.transform.CompileStatic
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.context.ConfigurableApplicationContext

import javax.annotation.Nonnull

/**
 * A simple helper class that given a Application Context and initial configuration conditions, will
 * return a ClientBatchJob instance with a valid {@link Job} and {@link JobParameters}
 */
@CompileStatic
class ClientBatchJob {

    public static final String PATH = "path"
    public static final String HOST = "host"
    public static final String PORT = "port"
    public static final String USERNAME = "username"
    public static final String PASSWORD = "password"

    final Job job
    final JobParameters jobParameters
    final JobLauncher jobLauncher

    protected ClientBatchJob(@Nonnull Job job, @Nonnull JobParameters jobParameters, @Nonnull JobLauncher jobLauncher) {
        if(job == null) throw new IllegalArgumentException("Job == null")
        if(jobParameters == null) throw new IllegalArgumentException("JobParameters == null")
        if(jobLauncher == null) throw new IllegalArgumentException("JobLauncher == null")

        this.job = job
        this.jobParameters = jobParameters
        this.jobLauncher = jobLauncher
    }

    public void run() {
        jobLauncher.run(job, jobParameters)
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
                serverBuilder.configAppContext.getBean("clientJob", Job),
                new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .addString(PATH, pathBuilder.path)
                        .addString(HOST, serverBuilder.host)
                        .addString(PORT, serverBuilder.port)
                        .addString(USERNAME, credentialsBuilder.username)
                        .addString(PASSWORD, credentialsBuilder.password)
                        .toJobParameters(),
                serverBuilder.configAppContext.getBean("jobLauncher", JobLauncher)
            )
        }
    }

}
