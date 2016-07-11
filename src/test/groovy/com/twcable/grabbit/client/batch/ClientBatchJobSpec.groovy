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

package com.twcable.grabbit.client.batch

import com.twcable.grabbit.DateUtil
import com.twcable.grabbit.GrabbitConfiguration
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Subject(ClientBatchJob)
class ClientBatchJobSpec extends Specification {

    @Shared
    def dateNow


    def setupSpec() {
        dateNow = new Date()
    }


    @Unroll
    def "Make sure ClientBatch job gets configured correctly"() {
        when:
        final appContext = Mock(ConfigurableApplicationContext)
        appContext.getBean(_ as String, JobOperator) >> Mock(JobOperator)
        final job = new ClientBatchJob.ServerBuilder(appContext)
            .andServer("host", "port")
            .andCredentials("clientUser", "serverUser", "serverPass")
            .andClientJobExecutions(jobExecutions)
            .andConfiguration(new GrabbitConfiguration.PathConfiguration(path, [], [], deleteBeforeWrite, pathDeltaContent, 100))
            .build()

        then:
        job != null
        job.jobParameters != null
        job.jobParameters.get(ClientBatchJob.PATH) == path
        job.jobParameters.get(ClientBatchJob.CONTENT_AFTER_DATE) == contentAfterDate
        job.jobParameters.get(ClientBatchJob.DELETE_BEFORE_WRITE).toBoolean() == deleteBeforeWrite
        job.jobParameters.get(ClientBatchJob.PATH_DELTA_CONTENT).toBoolean() == pathDeltaContent

        where:
        pathDeltaContent | path     | contentAfterDate                        | deleteBeforeWrite
        true             | "/path1" | DateUtil.getISOStringFromDate(dateNow)  | true
        false            | "/path1" | null                                    | false
        true             | "/path2" | null                                    | true
        false            | "/path2" | null                                    | false
    }


    def getJobExecutions() {
        def ex1 = new JobExecution(1, new JobParametersBuilder().addString("path", "/path1").toJobParameters())
        ex1.endTime = dateNow
        ex1.status = BatchStatus.COMPLETED
        def ex2 = new JobExecution(2, new JobParametersBuilder().addString("path", "/path2").toJobParameters())
        ex2.endTime = dateNow
        ex2.status = BatchStatus.FAILED

        [ex1, ex2]
    }
}
