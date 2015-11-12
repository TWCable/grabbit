package com.twcable.grabbit.client.batch.steps.validation

import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.client.batch.ClientBatchJobContext
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.StepExecution
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import javax.jcr.Node
import javax.jcr.PathNotFoundException
import javax.jcr.RepositoryException
import javax.jcr.Session

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

@Subject(ValidJobDecider)
class ValidJobDeciderSpec extends Specification {

    final StepExecution stepExecution = Mock(StepExecution)

    def cleanup() {
        ClientBatchJobContext.cleanup()
    }

    @Unroll
    def "Will pass validation if paths without a parent are submitted for path '#path'"() {
        given:
        final ValidJobDecider jobDecider = new ValidJobDecider()

        final JobExecution jobExecution = Mock(JobExecution) {
            getJobParameters() >> {
                return Mock(JobParameters) {
                    getString(ClientBatchJob.PATH) >> path
                }
            }
        }

        expect:
        jobDecider.decide(jobExecution, stepExecution) == ValidJobDecider.JOB_VALID

        where:
        path << ['/', '/foo', '/foo/']
    }

    def "Will fail validation if paths with a nonexistent parent are submitted"() {
        when:
        final JobExecution jobExecution = Mock(JobExecution) {
            getJobParameters() >> {
                return Mock(JobParameters) {
                    getString(ClientBatchJob.PATH) >> "/foo/bar"
                }
            }
        }
        final Session session = Mock(Session) {
            getNode("/foo") >> { throw new PathNotFoundException() }
        }

        ClientBatchJobContext.setSession(session)

        final ValidJobDecider jobDecider = new ValidJobDecider()

        then:
        jobDecider.decide(jobExecution, stepExecution) == ValidJobDecider.JOB_INVALID
    }

    def "Will pass validation if paths with an existent parent are submitted"(){
        when:
        final JobExecution jobExecution = Mock(JobExecution) {
            getJobParameters() >> {
                return Mock(JobParameters) {
                    getString(ClientBatchJob.PATH) >> "/foo/bar"
                }
            }
        }
        final Session session = Mock(Session) {
            getNode("/foo") >> { return Mock(Node) }
        }

        ClientBatchJobContext.setSession(session)


        final ValidJobDecider jobDecider = new ValidJobDecider()

        then:
        jobDecider.decide(jobExecution, stepExecution) == ValidJobDecider.JOB_VALID
    }

    def "Recovers gracefully if an issue is encountered with the repository"() {
        when:
        final JobExecution jobExecution = Mock(JobExecution) {
            getJobParameters() >> {
                return Mock(JobParameters) {
                    getString(ClientBatchJob.PATH) >> "/foo/bar"
                }
            }
        }
        final Session session = Mock(Session) {
            getNode("/foo") >> { throw new RepositoryException() }
        }

        ClientBatchJobContext.setSession(session)

        final ValidJobDecider jobDecider = new ValidJobDecider()

        then:
        jobDecider.decide(jobExecution, stepExecution) == ValidJobDecider.JOB_INVALID
    }

}
