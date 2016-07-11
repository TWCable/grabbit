package com.twcable.grabbit.client.batch.steps.workspace

import com.twcable.grabbit.client.batch.ClientBatchJobContext
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus
import spock.lang.Specification

import javax.jcr.Node
import javax.jcr.PathNotFoundException
import javax.jcr.Session

import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.repository

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

@SuppressWarnings("GroovyAccessibility")
class DeleteBeforeWriteTaskletSpec extends Specification {

    def cleanup() {
        ClientBatchJobContext.cleanup()
    }

    def "Exclude paths bean is set correctly"() {
        when:
        final deleteBeforeWriteTasklet = new DeleteBeforeWriteTasklet("/foo/bar", excludePaths)

        then:
        relativeExpectedExcludePaths == deleteBeforeWriteTasklet.relativeExcludePaths

        //Excluded paths are expected to be absolute from job configuration, but transformed to relative paths
        where:
        excludePaths                          |   relativeExpectedExcludePaths
        null                                  |   []
        "/foo/bar/foopath"                    |   ["foopath"]
        "/foo/bar/foopath "                   |   ["foopath"]
        "/foo/bar/foo/bar*/foo/bar/foo/doo"   |   ["foo/bar", "foo/doo"]
    }


    def "Job path bean is set correctly"() {
        when:
        final deleteBeforeWriteTasklet = new DeleteBeforeWriteTasklet(jobPath, null)

        then:
        expectedJobPath == deleteBeforeWriteTasklet.jobPath

        where:
        jobPath         |   expectedJobPath
        "foo/bar"       |   "/foo/bar"
        "/foo/bar "     |   "/foo/bar"
        "/foo/bar/"     |   "/foo/bar"
    }

    def "If no exclude paths, the entire path is deleted"() {
        given:
        final jobPath = "/foo/bar"

        when:
        final session = Mock(Session) {
            getNode(jobPath) >> Mock(Node) {
                1 * remove()
            }
            1 * save()
        }
        ClientBatchJobContext.setSession(session)

        final deleteBeforeWriteTasklet = new DeleteBeforeWriteTasklet(jobPath, null)

        then:
        RepeatStatus.FINISHED == deleteBeforeWriteTasklet.execute(Mock(StepContribution), Mock(ChunkContext))
    }

    def "If exclude paths are provided, we don't delete those paths"() {
        given:
        final repository = repository(
                node("root",
                    node("a",
                        node("a",
                            node("a"),
                            node("b"),
                            node("c")
                        ),
                        node("b")
                    ),
                    node("b",
                        node("a"),
                        node("b")
                    ),
                    node("c")
                )
        ).build()
        final session = repository.loginAdministrative(null)

        final excludePaths = ["/root/a/a/b", "/root/a/a/c", "/root/b"]
        final deleteBeforeWriteTasklet = new DeleteBeforeWriteTasklet("/root", excludePaths.join('*'))

        ClientBatchJobContext.setSession(session)


        when:
        deleteBeforeWriteTasklet.execute(Mock(StepContribution), Mock(ChunkContext))

        final excludedPathsNotDeleted = excludePaths.every {
            try {
                session.getNode(it)
                return true
            }
            catch(PathNotFoundException ex) {
                return false
            }
        }

        then:
        excludedPathsNotDeleted
    }


    def "If job path is marked for deletion, but it doesn't exist on client, we fail gracefully"() {
        given:
        final emptyRepository = repository().build()
        final session = emptyRepository.loginAdministrative(null)
        final deleteBeforeWriteTasklet = new DeleteBeforeWriteTasklet("/nonExistentRoot", null)

        ClientBatchJobContext.setSession(session)

        when:
        deleteBeforeWriteTasklet.execute(Mock(StepContribution), Mock(ChunkContext))

        then:
        notThrown(PathNotFoundException)
    }


    def "If a job path is marked for deletion, and an exclude path does not exist on the client, traversal for that path fails gracefully"() {
        given:
        final repository = repository(
                node("root",
                        node("a"),
                        node("b",
                                node("a"),
                                node("b")
                        ),
                        node("c")
                )
        ).build()
        final session = repository.loginAdministrative(null)

        final excludePaths = ["/root/a", "/root/b/c", "/root/d"]
        final deleteBeforeWriteTasklet = new DeleteBeforeWriteTasklet("/root", excludePaths.join('*'))

        ClientBatchJobContext.setSession(session)

        when:
        deleteBeforeWriteTasklet.execute(Mock(StepContribution), Mock(ChunkContext))

        then:
        notThrown(PathNotFoundException)
    }
}
