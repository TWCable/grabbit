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

package com.twcable.grabbit.client.batch.workflows

import com.day.cq.workflow.launcher.WorkflowLauncher
import com.twcable.grabbit.client.batch.workflows.impl.DefaultWorkFlowManager
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Subject(WorkflowManager)
class WorkflowManagerSpec extends Specification {

    def "Should manage multithreading correctly"() {
        setup:
        ExecutorService threadPool = Executors.newCachedThreadPool()
        WorkflowLauncher workflowLauncher = new StubWorkflowLauncher(5)
        WorkflowManager workflowManager = new DefaultWorkFlowManager(workflowLauncher: workflowLauncher)
        Collection<String> workflowIds = ['id1', 'id2', 'id3']

        //Simulates a WhiteList with 10 paths.
        //In this case (worst case for the most part), all of the 10 paths
        //Have requested the same 3 workflows to be turned off
        def callables = (1..10).collect { i ->
            new Callable<Void>() {
                @Override
                Void call() throws Exception {
                    workflowManager.turnOff(workflowIds)
                    Thread.sleep(new Random().nextInt(200))
                    workflowManager.turnOn(workflowIds)
                }
            }
        }

        when:
        workflowManager.activate()
        Collection<Future> futures = threadPool.invokeAll(callables)

        then:
        futures.each { Future future -> future.get(20, TimeUnit.SECONDS) }

        //Number of times editConfig is called must be equal to twice
        //the number of unique workflows that were requested to be turned off
        workflowLauncher.editConfigCallCount == workflowIds.size() * 2

        cleanup:
        workflowManager.deactivate()
        threadPool.shutdown()
        threadPool.awaitTermination(10, TimeUnit.SECONDS)
    }

}
