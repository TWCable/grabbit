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

import spock.lang.Specification
import spock.lang.Subject

import javax.jcr.Session

import java.util.concurrent.Callable as Job
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Subject(ClientBatchJobContext)
class ClientBatchJobContextSpec extends Specification {

    Job setSessionAndInputStreamOnJob(final Session session, final InputStream inputStream) {
        return new Job<List>() {
            @Override
            List call() throws Exception {
                ClientBatchJobContext.setSession(session)
                ClientBatchJobContext.setInputStream(inputStream)
                return [ClientBatchJobContext.session, ClientBatchJobContext.inputStream] as List
            }
        }
    }

    def "Jobs running on separate threads can set their own ClientBatchJobContext values"() {
        given:
        final Session jobOneSession = Mock(Session)
        final InputStream jobOneInputStream = Mock(InputStream)

        final Session jobTwoSession = Mock(Session)
        final InputStream jobTwoInputStream = Mock(InputStream)

        //This will set unique objects on each job's ClientBatchJobContext
        final Job jobOne = setSessionAndInputStreamOnJob(jobOneSession, jobOneInputStream)
        final Job jobTwo = setSessionAndInputStreamOnJob(jobTwoSession, jobTwoInputStream)

        //We should get a list of the same values back, unchanged by the other job's thread
        ExecutorService executorService = Executors.newFixedThreadPool(2)
        final List jobOneObjects = executorService.submit(jobOne).get()
        final List jobTwoObjects = executorService.submit(jobTwo).get()
        executorService.shutdown()

        expect:
        jobOneObjects.get(0).is jobOneSession
        jobOneObjects.get(1).is jobOneInputStream

        jobTwoObjects.get(0).is jobTwoSession
        jobTwoObjects.get(1).is jobTwoInputStream
    }

    def "Session, and InputStream are cleaned up correctly on cleanup()"() {
        given:
        final Session session = Mock(Session) {
            isLive() >> true
        }
        final InputStream inputStream = Mock(InputStream)

        when:
        ClientBatchJobContext.setSession(session)
        ClientBatchJobContext.setInputStream(inputStream)

        ClientBatchJobContext.cleanup()

        then:
        1 * session.logout()
        1 * inputStream.close()
    }
}
