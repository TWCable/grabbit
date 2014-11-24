package com.twc.grabbit.spring.batch.repository

import com.twc.jackalope.impl.sling.SimpleResourceResolverFactory
import org.apache.sling.api.resource.ResourceResolverFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.JobParameters
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static com.twc.grabbit.spring.batch.repository.JcrJobExecutionDao.*
import static com.twc.grabbit.spring.batch.repository.JcrStepExecutionDao.*
import static com.twc.jackalope.JCRBuilder.*

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
