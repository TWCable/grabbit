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
import org.springframework.batch.core.DefaultJobKeyGenerator
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParameters
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static JcrJobInstanceDao.*
import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property
import static com.twcable.jackalope.JCRBuilder.*

@Subject(JcrJobInstanceDao)
class JcrJobInstanceDaoSpec extends Specification {

    @Shared
    ResourceResolverFactory mockFactory

    def setupSpec() {
        final builder =
                node("var",
                    node("grabbit",
                        node("job",
                            node("repository",
                                node("jobInstances",
                                    node("1",
                                        property(INSTANCE_ID, 1),
                                        property(NAME, "someJob"),
                                        property(KEY, new DefaultJobKeyGenerator().generateKey(new JobParameters([someKey : new JobParameter("someValue")])))
                                    ),
                                    node("2",
                                        property(INSTANCE_ID, 2),
                                        property(NAME, "someOtherJob"),
                                    ),
                                    node("3",
                                        property(INSTANCE_ID, 3),
                                        property(NAME, "someOtherJob"),
                                    ),
                                    node("4",
                                        property(INSTANCE_ID, 4),
                                        property(NAME, "someOtherJob"),
                                    )
                                )
                            )
                        )
                    )
                )
        mockFactory = new SimpleResourceResolverFactory(repository(builder).build())
    }

    def "EnsureRootResource for JcrJobInstanceDao"() {
        when:
        final jobInstanceDao = new JcrJobInstanceDao(mockFactory)
        jobInstanceDao.ensureRootResource()

        then:
        notThrown(IllegalStateException)
    }

    def "GetJobInstance for given JobExecution"() {
        when:
        final jobInstanceDao = new JcrJobInstanceDao(mockFactory)
        final result = jobInstanceDao.getJobInstance(new JobExecution(new JobInstance(1, "someJob"), new JobParameters()))

        then:
        result != null
        result.id == 1

    }

    def "GetJobInstance for given InstanceId"() {
        when:
        final jobInstanceDao = new JcrJobInstanceDao(mockFactory)
        final result = jobInstanceDao.getJobInstance(1)

        then:
        result != null
        result.jobName == "someJob"

    }

    def "GetJobInstance for given Job Name and Job parameters"() {
        when:
        final jobInstanceDao = new JcrJobInstanceDao(mockFactory)
        final result = jobInstanceDao.getJobInstance("someJob", new JobParameters([someKey : new JobParameter("someValue")]))

        then:
        result != null
        result.id == 1
    }

    @Unroll
    def "GetJobInstances for given Job Name #jobName, a start index and count"() {
        when:
        final jobInstanceDao = new JcrJobInstanceDao(mockFactory)
        final result = jobInstanceDao.getJobInstances(jobName, 0, Integer.MAX_VALUE)

        then:
        result != null
        result.size() == size
        result.first().id == firstId

        where:
        jobName         | size  | firstId
        "someJob"       | 1     | 1
        "someOtherJob"  | 3     | 4
    }

    def "GetJobNames for Job Instances"() {
        when:
        final jobInstanceDao = new JcrJobInstanceDao(mockFactory)
        final result = jobInstanceDao.jobNames

        then:
        result.containsAll(["someJob", "someOtherJob"])
    }
}
