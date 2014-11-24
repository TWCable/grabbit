package com.twc.grabbit.spring.batch.repository

import com.twc.jackalope.impl.sling.SimpleResourceResolverFactory
import org.apache.sling.api.resource.ResourceResolverFactory
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.repository.ExecutionContextSerializer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static com.twc.grabbit.spring.batch.repository.JcrExecutionContextDao.*
import static com.twc.jackalope.JCRBuilder.node

import com.twc.jackalope.NodeBuilder
import static com.twc.jackalope.JCRBuilder.*

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
            [ deserialized : new String("Deserialized") ]
        }

        @Override
        void serialize(Object object, OutputStream outputStream) throws IOException {
            outputStream.write("Serialized".bytes)
        }
    }

}
