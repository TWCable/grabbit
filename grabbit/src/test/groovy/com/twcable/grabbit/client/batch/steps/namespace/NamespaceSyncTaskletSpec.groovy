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

package com.twcable.grabbit.client.batch.steps.namespace

import com.twcable.grabbit.client.batch.ClientBatchJobContext
import org.slf4j.Logger
import org.springframework.batch.repeat.RepeatStatus
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static com.twcable.jackalope.JCRBuilder.node
import static com.twcable.jackalope.JCRBuilder.repository

@Subject(NamespaceSyncTasklet)
class NamespaceSyncTaskletSpec extends Specification {

    def "WriteLock on Execute method should handle multi-threading correctly"() {
        setup:
        def threadPool = Executors.newCachedThreadPool()
        def tasklet = new NamespaceSyncTasklet()

        def logField = getLogFieldForTasklet()
        def originalValue = logField.get(NamespaceSyncTasklet)

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
        def logField = NamespaceSyncTasklet.getDeclaredField("log")

        //Get around "private" field access
        logField.accessible = true

        //Get rid of "final" by resetting the modifier for FINAL
        def modifiersField = Field.getDeclaredField("modifiers")
        modifiersField.accessible = true
        modifiersField.setInt(logField, logField.modifiers & ~Modifier.FINAL)
        return logField
    }
}
