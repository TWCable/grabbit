package com.twc.grabbit.spring.batch.repository

import com.twc.jackalope.impl.sling.SimpleResourceResolverFactory
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

import static com.twc.grabbit.spring.batch.repository.JcrJobInstanceDao.*
import static com.twc.jackalope.JCRBuilder.node
import static com.twc.jackalope.JCRBuilder.property
import com.twc.jackalope.NodeBuilder
import static com.twc.jackalope.JCRBuilder.*

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
