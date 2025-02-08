package ru.killwolfvlad.workflows.loadTest

import kotlinx.serialization.Serializable
import ru.killwolfvlad.workflows.activities.delayActivity
import ru.killwolfvlad.workflows.core.WorkflowsWorker
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.execute
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.withActivity
import kotlin.time.Duration.Companion.seconds

@Serializable
data class LoadTestWorkflowContext(
    val id: String,
    val payload: String,
)

@OptIn(WorkflowsPerformance::class)
class LoadTestWorkflow(
    private val doneCallback: (id: String, payload: String) -> Unit,
) : Workflow {
    companion object {
        fun getId(context: LoadTestWorkflowContext): WorkflowId = WorkflowId("loadTestWorkflow:${context.id}")
    }

    override suspend fun execute() {
        delayActivity("delay", 10.seconds)

        withActivity<LoadTestWorkflowContext, Unit>("done") { context ->
            doneCallback(context.id, context.payload)
        }
    }
}

suspend inline fun WorkflowsWorker.executeLoadTestWorkflow(context: LoadTestWorkflowContext) =
    execute<LoadTestWorkflow, LoadTestWorkflowContext>(LoadTestWorkflow.getId(context), context)
