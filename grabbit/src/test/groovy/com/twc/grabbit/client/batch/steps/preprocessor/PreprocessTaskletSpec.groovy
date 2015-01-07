package com.twc.grabbit.client.batch.steps.preprocessor

import com.twc.grabbit.client.batch.ClientBatchJobContext
import org.slf4j.Logger
import org.springframework.batch.repeat.RepeatStatus
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static com.twc.jackalope.JCRBuilder.node
import static com.twc.jackalope.JCRBuilder.repository

@Subject(PreprocessTasklet)
class PreprocessTaskletSpec extends Specification {

    def "WriteLock on Execute method should handle multi-threading correctly"() {
        setup:
        def threadPool = Executors.newCachedThreadPool()
        def tasklet = new PreprocessTasklet()

        def logField = getLogFieldForTasklet()
        def originalValue = logField.get(PreprocessTasklet)

        //Mock the logger API's info() method to use the custom StringWriter
        def infoLog = new StringWriter()
        def stringLogger = Mock(Logger)
        stringLogger.info(_) >> { String msg -> infoLog.append(msg) }
        logField.set(null, stringLogger)

        def repository = repository(node("foobar")).build()
        def session = repository.loginAdministrative(null)

        def callables = (1..30).collect {
            new Callable<RepeatStatus>() {
                @Override
                RepeatStatus call() throws Exception {
                    ClientBatchJobContext.THREAD_LOCAL.set(new ClientBatchJobContext(new ByteArrayInputStream(), session))
                    return tasklet.execute(null, null)
                }
            }
        }

        when:
        final futures = threadPool.invokeAll(callables)

        then:
        //Checks if all the Futures return RepeatStatus.FINISHED
        checkExecutionStatus(futures)
        //Test if the WriteLock works by looking at the order of the logged values in the StubStringLogger
        infoLog.toString() == "Start writing namespaces.Finished writing namespaces.".multiply(30)

        cleanup:
        //Reset the Logger's value to the Original Slf4j logger instance
        logField.set(null, originalValue)
        threadPool.shutdown()
        threadPool.awaitTermination(10, TimeUnit.SECONDS)

    }

    def checkExecutionStatus(def futures) {
        futures.every { it.get(20, TimeUnit.SECONDS) == RepeatStatus.FINISHED }
    }

    def getLogFieldForTasklet() {
        def logField = PreprocessTasklet.getDeclaredField("log")

        //Get around "private" field access
        logField.accessible = true

        //Get rid of "final" by resetting the modifier for FINAL
        def modifiersField = Field.getDeclaredField("modifiers")
        modifiersField.accessible = true
        modifiersField.setInt(logField, logField.modifiers & ~Modifier.FINAL)
        return logField
    }
}
