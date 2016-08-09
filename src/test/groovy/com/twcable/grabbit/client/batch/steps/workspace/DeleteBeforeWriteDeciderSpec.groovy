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
package com.twcable.grabbit.client.batch.steps.workspace

import com.twcable.grabbit.client.batch.ClientBatchJob
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.job.flow.FlowExecutionStatus
import spock.lang.Specification

class DeleteBeforeWriteDeciderSpec extends Specification {

    def "Decides when to delete before writing nodes correctly"() {
        when:
        final jobExecution = Mock(JobExecution) {
            getJobParameters() >> Mock(JobParameters){
                getString(ClientBatchJob.DELETE_BEFORE_WRITE) >> deleteBeforeWriteParameter
            }
        }

        final deleteBeforeWriteDecider = new DeleteBeforeWriteDecider()
        final flowExecutionStatus = deleteBeforeWriteDecider.decide(jobExecution, Mock(StepExecution))
        then:
        flowExecutionStatus == expectedFlowExecutionStatus

        where:
        deleteBeforeWriteParameter  |   expectedFlowExecutionStatus
        "true"                      |   new FlowExecutionStatus("YES")
        "false"                     |   new FlowExecutionStatus("NO")
    }
}
