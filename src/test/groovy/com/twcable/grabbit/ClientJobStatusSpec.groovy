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

package com.twcable.grabbit

import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.spring.batch.repository.GrabbitJobExecution
import org.springframework.batch.core.*
import org.springframework.batch.core.explore.JobExplorer
import spock.lang.Specification
import spock.lang.Unroll

class ClientJobStatusSpec extends Specification {

    def "IllegalArgumentException is thrown if invalid parameters are passed to get()"() {
        when:
        ClientJobStatus.get(null, 123L)

        then:
        thrown(IllegalArgumentException)
    }

    def "IllegalArgumentException is thrown if invalid parameters are passed to getAll()"() {
        when:
        ClientJobStatus.getAll(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "If client job status is requested for a job that can not be found, a NonExistentJobException is thrown"() {
        given:
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobExecution(123L) >> null
        }
        when:
        //123L is not a existing job identifier
        ClientJobStatus.get(jobExplorer, 123L)

        then:
        thrown(NonExistentJobException)
    }

    def "Get a job status for a currently running job"() {
        given:
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobExecution(123L) >> Mock(GrabbitJobExecution) {
                isRunning() >> true
                getId() >> 123L
                getStartTime() >> new Date(2015, 10, 18)
                getEndTime() >> new Date(2015, 10, 19)
                getExitStatus() >> ExitStatus.UNKNOWN
                getJobParameters() >> Mock(JobParameters) {
                    getString(ClientBatchJob.PATH) >> "/content/somecontent"
                }
                getStepExecutions() >> [
                    Mock(StepExecution) {
                        getStepName() >> "clientJcrNodes"
                        getWriteCount() >> 1000
                    }
                ]
            }
        }

        when:
        final ClientJobStatus jobStatus = ClientJobStatus.get(jobExplorer, 123L)

        then:
        jobStatus.jobExecutionId == 123L
        jobStatus.startTime == new Date(2015, 10, 18)
        jobStatus.endTime == new Date(2015, 10, 19)
        jobStatus.exitStatus == ExitStatus.UNKNOWN
        jobStatus.path == "/content/somecontent"
        jobStatus.timeTaken == -1L
        jobStatus.jcrNodesWritten == 1000
    }

    def "Get a job status for a completed job"() {
        given:
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobExecution(123L) >> Mock(GrabbitJobExecution) {
                isRunning() >> false
                getId() >> 123L
                getStartTime() >> new Date(2015, 10, 18)
                getEndTime() >> new Date(2015, 10, 19)
                getExitStatus() >> ExitStatus.COMPLETED
                getJobParameters() >> Mock(JobParameters) {
                    getString(ClientBatchJob.PATH) >> "/content/somecontent"
                }
                getStepExecutions() >> [
                        Mock(StepExecution) {
                            getStepName() >> "clientJcrNodes"
                            getWriteCount() >> 1000
                        }
                ]
            }
        }

        when:
        final ClientJobStatus jobStatus = ClientJobStatus.get(jobExplorer, 123L)

        then:
        jobStatus.jobExecutionId == 123L
        jobStatus.startTime == new Date(2015, 10, 18)
        jobStatus.endTime == new Date(2015, 10, 19)
        jobStatus.exitStatus == ExitStatus.COMPLETED
        jobStatus.path == "/content/somecontent"
        jobStatus.timeTaken == 86400000L
        jobStatus.jcrNodesWritten == 1000
    }

    def "If no jobs have ever been run, getAll returns an empty list"() {
        given:
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE) >> null
        }

        when:
        final Collection<ClientJobStatus> status = ClientJobStatus.getAll(jobExplorer)

        then:
        status.isEmpty()
    }

    def "If no jobs can be found with a transactionID, getAllForTransaction returns an empty list"() {
        given:
        final JobInstance jobInstance = Mock(JobInstance)
        //The job explorer only contains a single job with paired to a transactionID "789L"
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE) >> [
                    jobInstance
            ]
            getJobExecutions(jobInstance) >> [
                    Mock(JobExecution) {
                        getId() >> 123L
                    }
            ]
            getJobExecution(123L) >> Mock(GrabbitJobExecution) {
                getJobParameters() >> Mock(JobParameters)
                isRunning() >> false
                getEndTime() >> Mock(Date)
                getStartTime() >> Mock(Date)
                getTransactionID() >> 789L
            }
        }

        when:
        //We won't find any jobs with a transactionID of 456L
        final Collection<ClientJobStatus> status = ClientJobStatus.getAllForTransaction(jobExplorer, 456L)

        then:
        status.empty
    }

    def "If no jobs have ever been run, getAllTransactions returns an empty list"() {
        given:
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE) >> null
        }

        when:
        final Collection<String> transactionList = ClientJobStatus.getAllTransactions(jobExplorer)

        then:
        transactionList.empty
    }

    def "Get status of all jobs ever run"() {
        given:
        final JobInstance jobInstance = Mock(JobInstance)
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE) >> [
                jobInstance
            ]
            getJobExecutions(jobInstance) >> [
                Mock(JobExecution) {
                    getId() >> 123L
                }
            ]
            getJobExecution(123L) >> Mock(GrabbitJobExecution) {
                getJobParameters() >> Mock(JobParameters)
                isRunning() >> false
                getEndTime() >> Mock(Date)
                getStartTime() >> Mock(Date)
            }
        }

        when:
        final Collection<ClientJobStatus> status = ClientJobStatus.getAll(jobExplorer)

        then:
        status.size() == 1
    }

    def "Get status for jobs in a transaction"() {
        given:
        final JobInstance jobInstance = Mock(JobInstance)
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE) >> [
                    jobInstance
            ]
            getJobExecutions(jobInstance) >> [
                    Mock(JobExecution) {
                        getId() >> 123L
                    }
            ]
            getJobExecution(123L) >> Mock(GrabbitJobExecution) {
                getJobParameters() >> Mock(JobParameters)
                isRunning() >> false
                getEndTime() >> Mock(Date)
                getStartTime() >> Mock(Date)
                getTransactionID() >> 456L
            }
        }

        when:
        final Collection<ClientJobStatus> status = ClientJobStatus.getAllForTransaction(jobExplorer, 456L)

        then:
        status.size() == 1
        status.get(0).transactionID == 456L
    }

    @Unroll
    def "If there are different jobs with same transaction id, then getAllTransactions will return unique Collection of transaction ids"() {
        given:
        final JobInstance jobInstance = Mock(JobInstance)
        final JobInstance anotherJobInstance = Mock(JobInstance)
        final JobExplorer jobExplorer = Mock(JobExplorer) {
            getJobInstances(ClientBatchJob.JOB_NAME, 0, Integer.MAX_VALUE) >> [
                    jobInstance,anotherJobInstance
            ]
            getJobExecutions(jobInstance) >> [
                    Mock(JobExecution) {
                        getId() >> jobId
                    }
            ]
            getJobExecutions(anotherJobInstance) >> [
                    Mock(JobExecution) {
                        getId() >> anotherJobId
                    }
            ]
            getJobExecution(jobId) >> Mock(GrabbitJobExecution) {
                getJobParameters() >> Mock(JobParameters)
                isRunning() >> false
                getEndTime() >> Mock(Date)
                getStartTime() >> Mock(Date)
                getTransactionID() >> transactionId
            }
            getJobExecution(anotherJobId) >> Mock(GrabbitJobExecution) {
                getJobParameters() >> Mock(JobParameters)
                isRunning() >> false
                getEndTime() >> Mock(Date)
                getStartTime() >> Mock(Date)
                getTransactionID() >> anotherTransactionId
            }
        }

        when:
        final Collection<String> status = ClientJobStatus.getAllTransactions(jobExplorer)

        then:
        status.size() == size

        where:
        jobId << [1L, 2L]
        anotherJobId << [3L, 4L]
        transactionId << [100L, 100L]
        anotherTransactionId << [100L, 101L]
        size << [1, 2]
    }
}
