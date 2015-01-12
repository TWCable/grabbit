package com.twc.grabbit.client.batch

import com.twc.grabbit.DateUtil
import com.twc.grabbit.GrabbitConfiguration
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Subject(ClientBatchJob)
class ClientBatchJobSpec extends Specification {

    @Unroll
    def "Make sure ClientBatch job gets configured correctly"() {
        when:
        final appContext = Mock(ConfigurableApplicationContext)
        appContext.getBean(_ as String, JobOperator) >> Mock(JobOperator)
        final job = new ClientBatchJob.ServerBuilder(appContext)
                .andServer("host", "port")
        .andCredentials("user", "pass")
        .andDoDeltaContent(doDeltaContent)
        .andClientJobExecutions(jobExecutions)
        .andConfiguration(new GrabbitConfiguration.PathConfiguration(path, []))
        .build()

        then:
        job != null
        job.jobParameters != null
        job.jobParameters.get("${ClientBatchJob.PATH}") == path
        job.jobParameters.get("${ClientBatchJob.CONTENT_AFTER_DATE}") == contentAfterDate

        where:
        doDeltaContent | path     | contentAfterDate
        true           | "/path1" | "2015-01-19T15:04:24.387-05:00"
        false          | "/path1" | null
        true           | "/path2" | null
        false          | "/path2" | null

    }

    def getJobExecutions() {
        def ex1 = new JobExecution(1, new JobParametersBuilder().addString("path", "/path1").toJobParameters())
        ex1.endTime = DateUtil.getDateFromISOString("2015-01-19T15:04:24.387-05:00")
        ex1.status = BatchStatus.COMPLETED
        def ex2 = new JobExecution(2, new JobParametersBuilder().addString("path", "/path2").toJobParameters())
        ex2.endTime = DateUtil.getDateFromISOString("2015-01-19T15:04:24.387-05:00")
        ex2.status = BatchStatus.FAILED

        [ ex1, ex2 ]
    }
}
