package ru.killwolfvlad.workflows.core

import kotlinx.coroutines.delay
import ru.killwolfvlad.workflows.WorkflowsDescribeSpec
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.types.WorkflowId
import kotlin.time.Duration.Companion.seconds

class TestWorkflow : Workflow {
    override suspend fun execute() {
        delay(5.seconds)
        println("Hello world!")
    }
}

class WorkflowsWorkerTest : WorkflowsDescribeSpec({
    lateinit var workflowsWorker: WorkflowsWorker

    beforeSpec {
        workflowsWorker = WorkflowsWorker(
            defaultWorkflowConfig,
            defaultKeyValueClient,
            defaultWorkflowsClassManager,
            defaultWorkflowsExceptionHandler,
        )
    }

    beforeEach {
        workflowsWorker.init()
    }

    it("run workflow") {
        workflowsWorker.run<TestWorkflow>(WorkflowId("testWorkflow"), emptyMap())

        delay(10.seconds)
    }
})
