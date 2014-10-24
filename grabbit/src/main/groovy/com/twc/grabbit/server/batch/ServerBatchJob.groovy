package com.twc.grabbit.server.batch

import groovy.transform.CompileStatic
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.context.ConfigurableApplicationContext

import javax.annotation.Nonnull
import javax.jcr.Node as JcrNode
import javax.servlet.ServletOutputStream

/**
 * A simple helper class that given a Application Context and initial configuration conditions, will
 * return a ServerBatchJob instance with a valid {@link Job} and {@link JobParameters}
 */
@CompileStatic
class ServerBatchJob {

    public static final String PATH = "path"

    private final Job job
    private final JobParameters jobParameters
    private final JobLauncher jobLauncher

    public void run() {
        jobLauncher.run(job, jobParameters)
    }

    protected ServerBatchJob(@Nonnull Job job, @Nonnull JobParameters jobParameters, @Nonnull JobLauncher jobLauncher) {
        if(job == null) throw new IllegalArgumentException("Server job must be set.")
        if(jobParameters == null) throw new IllegalArgumentException("Server jobParameters must be set.")
        if(jobLauncher == null) throw new IllegalArgumentException("Server jobLauncher must be set.")

        this.job = job
        this.jobParameters = jobParameters
        this.jobLauncher = jobLauncher
    }

    // **********************************************************************
    // INNER CLASSES
    // **********************************************************************

    static class ConfigurationBuilder {
        final ConfigurableApplicationContext configAppContext
        Iterator<Map.Entry<String, String>> namespacesIterator
        Iterator<JcrNode> nodeIterator
        ServletOutputStream servletOutputStream

        ConfigurationBuilder(@Nonnull ConfigurableApplicationContext configurableApplicationContext) {
            this.configAppContext = configurableApplicationContext
        }

        PathBuilder andConfiguration(Iterator<Map.Entry<String, String>> namespacesIterator,
                                     Iterator<JcrNode> nodeIterator, ServletOutputStream servletOutputStream) {
            if(namespacesIterator == null) throw new IllegalArgumentException("namespacesIterator == null")
            if(nodeIterator == null) throw new IllegalArgumentException("nodeIterator == null")
            if(servletOutputStream == null) throw new IllegalArgumentException("servletOutputStream == null")

            this.namespacesIterator = namespacesIterator
            this.nodeIterator = nodeIterator
            this.servletOutputStream = servletOutputStream
            return new PathBuilder(this)
        }
    }

    static class PathBuilder {
        final ConfigurationBuilder configurationBuilder
        String path

        PathBuilder(ConfigurationBuilder configurationBuilder) {
            this.configurationBuilder = configurationBuilder
        }

        Builder andPath(String path) {
            if(path == null) throw new IllegalArgumentException("path == null")
            this.path = path
            return new Builder(this)
        }
    }

    @CompileStatic
    static class Builder {
        final ConfigurationBuilder configurationBuilder
        final PathBuilder pathBuilder

        protected Builder(PathBuilder pathBuilder) {
            this.pathBuilder = pathBuilder
            this.configurationBuilder = pathBuilder.configurationBuilder
        }

        ServerBatchJob build() {

            ServerBatchJobContext serverBatchJobContext =
                    new ServerBatchJobContext(
                            configurationBuilder.servletOutputStream,
                            configurationBuilder.namespacesIterator,
                            configurationBuilder.nodeIterator
                    )
            ServerBatchJobContext.THREAD_LOCAL.set(serverBatchJobContext)

            return new ServerBatchJob(
                    (Job) configurationBuilder.configAppContext.getBean("serverJob", Job),
                    new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString(PATH, pathBuilder.path)
                    .toJobParameters(),
                    (JobLauncher) configurationBuilder.configAppContext.getBean("serverJobLauncher" ,JobLauncher)
            )
        }

    }

}
