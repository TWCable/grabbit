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
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.repository.ExecutionContextSerializer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static com.twcable.grabbit.spring.batch.repository.JcrExecutionContextDao.EXECUTION_CONTEXT
import static com.twcable.grabbit.spring.batch.repository.JcrExecutionContextDao.EXECUTION_ID
import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.property
import static com.twcable.jackalope.JCRBuilder.repository

@Subject(JcrExecutionContextDao)
class JcrExecutionContextDaoSpec extends Specification {

    @Shared
    ResourceResolverFactory mockFactory

    @Shared
    ExecutionContextSerializer stubSerializer


    def setupSpec() {
        final builder =
            node("var",
                node("grabbit",
                    node("job",
                        node("repository",
                            node("executionContexts",
                                node("job",
                                    node("1",
                                        property(EXECUTION_ID, 1),
                                        property(EXECUTION_CONTEXT, "SomeThing")
                                    )
                                ),
                                node("step",
                                    node("1",
                                        property(EXECUTION_ID, 1),
                                        property(EXECUTION_CONTEXT, "SomeThing")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        mockFactory = new SimpleResourceResolverFactory(repository(builder).build())
        stubSerializer = new StubExecutionContextSerializer()
    }


    def "EnsureRootResource for JcrExecutionContextDao"() {
        when:
        final executionContextDao = new JcrExecutionContextDao(mockFactory, stubSerializer)
        executionContextDao.ensureRootResource()

        then:
        notThrown(IllegalStateException)

    }


    def "GetExecutionContext for a JobExecution"() {
        when:
        final executionContextDao = new JcrExecutionContextDao(mockFactory, stubSerializer)
        final result = executionContextDao.getExecutionContext(new JobExecution(1))

        then:
        result != null
        result.containsKey("deserialized")
    }


    def "GetExecutionContext for a StepExecution"() {
        when:
        final executionContextDao = new JcrExecutionContextDao(mockFactory, stubSerializer)
        final result = executionContextDao.getExecutionContext(new StepExecution("someStep", new JobExecution(1), 1))

        then:
        result != null
        result.containsKey("deserialized")
    }


    class StubExecutionContextSerializer implements ExecutionContextSerializer {

        @Override
        Object deserialize(InputStream inputStream) throws IOException {
            [deserialized: new String("Deserialized")]
        }


        @Override
        void serialize(Object object, OutputStream outputStream) throws IOException {
            outputStream.write("Serialized".bytes)
        }
    }

}
