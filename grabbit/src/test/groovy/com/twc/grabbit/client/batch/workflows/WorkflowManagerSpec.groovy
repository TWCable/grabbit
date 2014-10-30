package com.twc.grabbit.client.batch.workflows

import com.day.cq.workflow.launcher.WorkflowLauncher
import com.twc.grabbit.client.batch.workflows.impl.DefaultWorkFlowManager
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.*

@Subject(WorkflowManager)
class WorkflowManagerSpec extends Specification {

    def "Should manage multithreading correctly"() {
        setup:
        ExecutorService threadPool = Executors.newCachedThreadPool()
        WorkflowLauncher workflowLauncher = new StubWorkflowLauncher(5)
        WorkflowManager workflowManager = new DefaultWorkFlowManager(workflowLauncher: workflowLauncher)
        Collection<String> workflowIds = ['id1','id2','id3']

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
