/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twcable.grabbit.server.batch

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
    public static final String CONTENT_AFTER_DATE = "contentAfterDate"
    public static final String EXCLUDE_PATHS = "excludePaths"

    private final Job job
    private final JobParameters jobParameters
    private final JobLauncher jobLauncher


    public void run() {
        jobLauncher.run(job, jobParameters)
    }


    protected ServerBatchJob(@Nonnull Job job, @Nonnull JobParameters jobParameters, @Nonnull JobLauncher jobLauncher) {
        if (job == null) throw new IllegalArgumentException("Server job must be set.")
        if (jobParameters == null) throw new IllegalArgumentException("Server jobParameters must be set.")
        if (jobLauncher == null) throw new IllegalArgumentException("Server jobLauncher must be set.")

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
                                     Iterator<JcrNode> nodeIterator,
                                     ServletOutputStream servletOutputStream) {
            if (namespacesIterator == null) throw new IllegalArgumentException("namespacesIterator == null")
            if (nodeIterator == null) throw new IllegalArgumentException("nodeIterator == null")
            if (servletOutputStream == null) throw new IllegalArgumentException("servletOutputStream == null")

            this.namespacesIterator = namespacesIterator
            this.nodeIterator = nodeIterator
            this.servletOutputStream = servletOutputStream
            return new PathBuilder(this)
        }
    }

    static class PathBuilder {
        final ConfigurationBuilder configurationBuilder
        String path
        Collection<String> excludePaths


        PathBuilder(ConfigurationBuilder configurationBuilder) {
            this.configurationBuilder = configurationBuilder
        }

        ContentAfterDateBuilder andPath(String path, Collection<String> excludePaths) {
            if(path == null) throw new IllegalArgumentException("path == null")
            this.path = path
            this.excludePaths = (excludePaths == null || excludePaths.isEmpty()) ? (Collection<String>)Collections.EMPTY_LIST : excludePaths
            return new ContentAfterDateBuilder(this)
        }
    }

    static class ContentAfterDateBuilder {
        final PathBuilder pathBuilder
        String contentAfterDate


        ContentAfterDateBuilder(PathBuilder pathBuilder) {
            this.pathBuilder = pathBuilder
        }


        Builder andContentAfterDate(String dateString) {
            this.contentAfterDate = dateString
            return new Builder(this)
        }
    }

    @CompileStatic
    static class Builder {
        final ConfigurationBuilder configurationBuilder
        final ContentAfterDateBuilder afterDateBuilder


        protected Builder(ContentAfterDateBuilder afterDateBuilder) {
            this.afterDateBuilder = afterDateBuilder
            this.configurationBuilder = afterDateBuilder.pathBuilder.configurationBuilder
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
                    .addString(PATH, afterDateBuilder.pathBuilder.path)
                    .addString(EXCLUDE_PATHS, afterDateBuilder.pathBuilder.excludePaths.join("*"))
                    .addString(CONTENT_AFTER_DATE, afterDateBuilder.contentAfterDate)
                    .toJobParameters(),
                (JobLauncher) configurationBuilder.configAppContext.getBean("serverJobLauncher" ,JobLauncher)
            )
        }

    }

}
