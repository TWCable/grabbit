package com.twc.grabbit.spring.batch.repository

import com.twc.jackalope.impl.sling.SimpleResourceResolverFactory
import org.apache.sling.api.resource.ResourceResolverFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static com.twc.grabbit.spring.batch.repository.JcrJobExecutionDao.*
import static com.twc.jackalope.JCRBuilder.node
import static com.twc.jackalope.JCRBuilder.property
import com.twc.jackalope.NodeBuilder
import static com.twc.jackalope.JCRBuilder.*


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
