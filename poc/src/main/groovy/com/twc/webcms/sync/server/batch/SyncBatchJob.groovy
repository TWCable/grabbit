package com.twc.webcms.sync.server.batch

import com.twc.webcms.sync.server.batch.steps.jcrnodes.JcrNodesReader
import com.twc.webcms.sync.server.batch.steps.jcrnodes.JcrNodesWriter
import com.twc.webcms.sync.server.batch.steps.preprocessor.PreprocessReader
import com.twc.webcms.sync.server.batch.steps.preprocessor.PreprocessWriter
import groovy.transform.CompileStatic
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.context.ConfigurableApplicationContext

import javax.jcr.Node as JcrNode
import javax.annotation.Nonnull
import javax.servlet.ServletOutputStream

/**
 * A simple helper class that given a Application Context and initial configuration conditions, will
 * return a SyncBatchJob instance with a valid {@link Job} and {@link JobParameters}
 */
@CompileStatic
class SyncBatchJob {

    private final Job job
    private final JobParameters jobParameters

    public Job getJob() {
        if(job == null) throw new IllegalStateException("job must be set.")
        job
    }

    public JobParameters getJobParameters() {
        if(jobParameters == null) throw new IllegalStateException("jobParameters must be set.")
        jobParameters
    }

    protected SyncBatchJob(Job job, JobParameters jobParameters) {
        this.job = job
        this.jobParameters = jobParameters
    }

    public static Builder withApplicationContext(@Nonnull ConfigurableApplicationContext configurableApplicationContext) {
        if(configurableApplicationContext == null) throw new IllegalArgumentException("configurableApplicationContext == null")

        return new Builder(configurableApplicationContext)
    }

    // **********************************************************************
    // INNER CLASS
    // **********************************************************************

    @CompileStatic
    static class Builder {
        private ConfigurableApplicationContext configAppContext

        protected Builder(ConfigurableApplicationContext configurableApplicationContext) {
            this.configAppContext = configurableApplicationContext
        }

        Builder configureSteps(@Nonnull Iterator<Map.Entry<String, String>> namespacesIterator,
                               @Nonnull Iterator<JcrNode> nodeIterator,
                               @Nonnull ServletOutputStream servletOutputStream) {
            if(namespacesIterator == null) throw new IllegalArgumentException("namespacesIterator == null")
            if(nodeIterator == null) throw new IllegalArgumentException("nodeIterator == null")
            if(servletOutputStream == null) throw new IllegalArgumentException("servletOutputStream == null")

            syncPreprocessorStep(namespacesIterator, servletOutputStream)
            syncJcrNodesStep(nodeIterator, servletOutputStream)

            return this
        }

        SyncBatchJob build() {
            return new SyncBatchJob(
                    (Job) configAppContext.getBean("syncJob"),
                    new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters()
            )
        }

        /**
         * Sets {@link PreprocessReader#namespaces} and {@link PreprocessWriter#servletOutputStream}
         */
        private void syncPreprocessorStep(Iterator<Map.Entry<String, String>> namespacesIterator,
                                          ServletOutputStream servletOutputStream) {
            PreprocessReader preprocessReader = (PreprocessReader) configAppContext.getBean(PreprocessReader)
            preprocessReader.setNamespaces(namespacesIterator)
            PreprocessWriter preprocessWriter = (PreprocessWriter) configAppContext.getBean(PreprocessWriter)
            preprocessWriter.setServletOutputStream(servletOutputStream)
        }

        /**
         * Sets {@link JcrNodesReader#nodeIterator} and {@link JcrNodesWriter#servletOutputStream}
         */
        private void syncJcrNodesStep(Iterator<JcrNode> nodeIterator, ServletOutputStream servletOutputStream) {
            JcrNodesReader jcrNodesReader = (JcrNodesReader) configAppContext.getBean(JcrNodesReader)
            jcrNodesReader.setNodeIterator(nodeIterator)
            JcrNodesWriter jcrNodesWriter = (JcrNodesWriter) configAppContext.getBean(JcrNodesWriter)
            jcrNodesWriter.setServletOutputStream(servletOutputStream)
        }

    }

}
