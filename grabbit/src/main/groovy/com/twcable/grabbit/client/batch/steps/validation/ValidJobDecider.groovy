package com.twcable.grabbit.client.batch.steps.validation

import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.client.batch.ClientBatchJobContext
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.job.flow.FlowExecutionStatus
import org.springframework.batch.core.job.flow.JobExecutionDecider

import javax.jcr.PathNotFoundException
import javax.jcr.RepositoryException
import javax.jcr.Session

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

/**
 * This class serves as a validation gate for jobs in-flight.  It should be the first step on the client when running a job to determine
 * if the job is job that is safe, and valid to execute.
 */
@CompileStatic
@Slf4j
class ValidJobDecider implements JobExecutionDecider {

    final static FlowExecutionStatus JOB_VALID = new FlowExecutionStatus("VALID")
    final static FlowExecutionStatus JOB_INVALID = new FlowExecutionStatus("INVALID")


    private Session theSession() {
        ClientBatchJobContext.session
    }

    /**
     * Determines if the job to be executed is a valid job
     *
     * For example, are we being asked to sync a path for which a path has no existing parents?
     * If so, we should label this scenario as an invalid job; and not attempt to write "dirty data"
     *
     * @param jobExecution a job execution
     * @param stepExecution the latest step execution (may be null)
     * @return the exit status code.  Is it a valid or invalid job?
     */
    @Override
    FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        String jobPath = jobExecution.jobParameters.getString(ClientBatchJob.PATH)
        //For processing, remove trailing /
        jobPath = jobPath.replaceFirst(/\/$/, '')
        //Get the parent's path (if applicable) and determine if it exists already
        final parts = jobPath.split('/')
        //No parent, so nothing to worry about
        if(parts.length <= 2) return JOB_VALID

        final parentPath = jobPath - "/${parts[-1]}"
        final Session session = theSession()
        try {
            session.getNode(parentPath)
        } catch(PathNotFoundException pathException) {
            log.warn "${jobPath} is not a valid job path.  Make sure a parent is synched or created before this job is run"
            log.debug pathException.toString()
            return JOB_INVALID
        }
        catch(RepositoryException repoException) {
            log.error "${RepositoryException.class.canonicalName} Something went wrong when accessing the repository at ${this.class.canonicalName} for job path ${jobPath}!"
            log.error repoException.toString()
            return JOB_INVALID
        }
        log.debug "${ValidJobDecider.class.canonicalName} Job determined to be valid for job path ${jobPath}"
        return JOB_VALID
    }

}
