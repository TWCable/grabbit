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
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.CREATE_TIME
import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.END_TIME
import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.EXECUTION_ID
import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.EXIT_CODE
import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.EXIT_MESSAGE
import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.INSTANCE_ID
import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.JOB_NAME
import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.STATUS
import static com.twcable.grabbit.spring.batch.repository.JcrJobExecutionDao.VERSION
import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property
import static com.twcable.jackalope.JCRBuilder.repository

@Subject(JcrJobExecutionDao)
class JcrJobExecutionDaoSpec extends Specification {

    @Shared
    ResourceResolverFactory mockFactory


    def setupSpec() {
        final builder =
            node("var",
                node("grabbit",
                    node("job",
                        node("repository",
                            node("jobExecutions",
                                node("1",
                                    property(INSTANCE_ID, 1),
                                    property(EXECUTION_ID, 1),
                                    property(STATUS, "COMPLETED"),
                                    property(EXIT_CODE, "code"),
                                    property(EXIT_MESSAGE, "message"),
                                    property(CREATE_TIME, "2014-12-27T16:59:18.669-05:00"),
                                    property(END_TIME, "2014-12-29T16:59:18.669-05:00"),
                                    property(JOB_NAME, "someJob"),
                                    property(VERSION, 1)
                                ),
                                node("2",
                                    property(INSTANCE_ID, 1),
                                    property(EXECUTION_ID, 2),
                                    property(STATUS, "STARTED"),
                                    property(EXIT_CODE, "code"),
                                    property(EXIT_MESSAGE, "message"),
                                    property(CREATE_TIME, "2014-12-28T16:59:18.669-05:00"),
                                    property(END_TIME, "NULL"),
                                    property(JOB_NAME, "someJob")
                                ),
                                node("3",
                                    property(INSTANCE_ID, 2),
                                    property(EXECUTION_ID, 3),
                                    property(STATUS, "STARTED"),
                                    property(EXIT_CODE, "code"),
                                    property(EXIT_MESSAGE, "message"),
                                    property(CREATE_TIME, "2014-12-29T16:59:18.669-05:00"),
                                    property(END_TIME, "NULL"),
                                    property(JOB_NAME, "someOtherJob")
                                )
                            ),
                            node("jobInstances",
                                node("1"))
                        )
                    )
                )
            )
        mockFactory = new SimpleResourceResolverFactory(repository(builder).build())
    }


    def "EnsureRootResource for JcrJobExecutionDao"() {
        when:
        final jobExecutionDao = new JcrJobExecutionDao(mockFactory)
        jobExecutionDao.ensureRootResource()

        then:
        notThrown(IllegalStateException)

    }


    def "FindJobExecutions for given JobInstance"() {
        when:
        final jobExecutionDao = new JcrJobExecutionDao(mockFactory)
        final result = jobExecutionDao.findJobExecutions(new JobInstance(1, "someJob"))

        then:
        result != null
        result.size() == 2
        result.first().id == 2
    }


    def "GetLastJobExecution for given JobInstance"() {
        when:
        final jobExecutionDao = new JcrJobExecutionDao(mockFactory)
        final result = jobExecutionDao.getLastJobExecution(new JobInstance(1, "someJob"))

        then:
        result != null
        result.id == 2

    }


    def "FindRunningJobExecutions for given Job Name"() {
        when:
        final jobExecutionDao = new JcrJobExecutionDao(mockFactory)
        final result = jobExecutionDao.findRunningJobExecutions("someJob")

        then:
        result != null
        result.size() == 1
        result.first().id == 2
    }


    def "GetJobExecution for given JobExecution id"() {
        when:
        final jobExecutionDao = new JcrJobExecutionDao(mockFactory)
        final result = jobExecutionDao.getJobExecution(2)

        then:
        result != null
        result.jobId == 1
        result.id == 2
        result.status == BatchStatus.valueOf("STARTED")
    }


    def "SynchronizeStatus for a given JobExecution"() {
        when:
        final jobExecutionDao = new JcrJobExecutionDao(mockFactory)
        def unsyncronized = new JobExecution(1)
        unsyncronized.setVersion(0)
        unsyncronized.setStatus(BatchStatus.STARTED)
        jobExecutionDao.synchronizeStatus(unsyncronized)

        then:
        unsyncronized.version == 1
        unsyncronized.status == BatchStatus.COMPLETED
    }
}
