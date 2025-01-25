//package ru.killwolfvlad.workflows.core
//
//import kotlinx.coroutines.delay
//import ru.killwolfvlad.workflows.WorkflowsDescribeSpec
//import ru.killwolfvlad.workflows.activities.delayActivity
//import ru.killwolfvlad.workflows.activities.withActivity
//import ru.killwolfvlad.workflows.core.interfaces.Workflow
//import ru.killwolfvlad.workflows.core.types.WorkflowId
//import kotlin.time.Duration.Companion.seconds
//
//class TestWorkflow : Workflow {
//    override suspend fun execute() {
//        delayActivity("timer", 5.seconds)
//
//        withActivity("hello") {
//            println("Hello world!")
//
//            null
//        }
//    }
//}
//
//class WorkflowsWorkerTest : WorkflowsDescribeSpec({
//    lateinit var workflowsWorker: WorkflowsWorker
//
//    beforeSpec {
//        workflowsWorker = WorkflowsWorker(
//            defaultWorkflowsConfig,
//            defaultKeyValueClient,
//            defaultWorkflowsClassManager,
//            defaultWorkflowsExceptionHandler,
//        )
//    }
//
//    beforeEach {
//        workflowsWorker.init()
//    }
//
//    it("run workflow") {
//        workflowsWorker.run<TestWorkflow>(WorkflowId("testWorkflow"), emptyMap())
//
//        delay(10.seconds)
//    }
//})
