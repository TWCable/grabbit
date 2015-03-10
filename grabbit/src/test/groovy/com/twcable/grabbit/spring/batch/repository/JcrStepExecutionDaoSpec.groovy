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

package com.twcable.grabbit.spring.batch.repository

import com.twcable.jackalope.impl.sling.SimpleResourceResolverFactory
import org.apache.sling.api.resource.ResourceResolverFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.JobParameters
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static JcrStepExecutionDao.*
import static com.twcable.jackalope.JCRBuilder.*

@Subject(JcrStepExecutionDao)
class JcrStepExecutionDaoSpec extends Specification {

    @Shared
    ResourceResolverFactory mockFactory

    def setupSpec() {
        final builder =
                node("var",
                    node("grabbit",
                        node("job",
                            node("repository",
                                node("jobExecutions"),
                                node("stepExecutions",
                                    node("1",
                                        property(ID, 1),
                                        property(NAME, "someStep"),
                                        property(JOB_EXECUTION_ID, 1),
                                        property(STATUS, "COMPLETED"),
                                    ),
                                    node("5",
                                        property(ID, 5),
                                        property(NAME, "someOtherStep"),
                                        property(JOB_EXECUTION_ID, 3),
                                        property(STATUS, "STARTED"),
                                    )
                                )
                            )
                        )
                    )
                )
        mockFactory = new SimpleResourceResolverFactory(repository(builder).build())
    }

    def "EnsureRootResource for JcrStepExecutionDao"() {
        when:
        final stepExecutionDao = new JcrStepExecutionDao(mockFactory)
        stepExecutionDao.ensureRootResource()

        then:
        notThrown(IllegalStateException)
    }

    @Unroll
    def "GetStepExecution for a given JobExecution and a StepExecution id #stepExecutionId"() {
        when:
        final stepExecutionDao = new JcrStepExecutionDao(mockFactory)
        final result = stepExecutionDao.getStepExecution(new JobExecution(new JobInstance(1, "someJob"), jobExecutionId, new JobParameters()), stepExecutionId)

        then:
        result != null
        result.jobExecutionId == jobExecutionId
        result.status == stepStatus

        where:
        stepExecutionId  | jobExecutionId | stepStatus
        1                | 1              | BatchStatus.COMPLETED
        5                | 3              | BatchStatus.STARTED

    }
}
