package ru.killwolfvlad.workflows.loadTest

import kotlinx.serialization.Serializable
import ru.killwolfvlad.workflows.activities.delayActivity
import ru.killwolfvlad.workflows.core.WorkflowsWorker
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.withActivity
import kotlin.time.Duration.Companion.seconds

class LoadTestWorkflow(
    private val doneCallback: (id: String, payload: String) -> Unit,
) : Workflow {
    @Serializable
    data class Context(
        val id: String,
        val payload: String,
    )

    companion object {
        fun getId(context: Context): WorkflowId = WorkflowId("loadTestWorkflow:${context.id}")
    }

    override suspend fun execute() {
        delayActivity("delay", 10.seconds)

        withActivity<Context, Unit>("done") { context ->
            doneCallback(context.id, context.payload)
        }
    }
}

suspend inline fun WorkflowsWorker.executeLoadTestWorkflow(context: LoadTestWorkflow.Context) =
    execute<LoadTestWorkflow, LoadTestWorkflow.Context>(LoadTestWorkflow.getId(context), context)
