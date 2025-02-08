package ru.killwolfvlad.workflows.exampleApp

import kotlinx.serialization.Serializable
import ru.killwolfvlad.workflows.activities.delayActivity
import ru.killwolfvlad.workflows.core.WorkflowsWorker
import ru.killwolfvlad.workflows.core.execute
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.withActivity
import kotlin.time.Duration.Companion.minutes

@Serializable
data class ExampleWorkflowContext(
    val chatId: Long,
    val messageId: Long,
)

class ExampleWorkflow : Workflow {
    companion object {
        fun getId(context: ExampleWorkflowContext): WorkflowId = WorkflowId("exampleWorkflow:${context.chatId}:${context.messageId}")
    }

    override suspend fun execute() {
        delayActivity("delay", 1.minutes)

        withActivity<ExampleWorkflowContext, Unit>("deleteMessage") { context ->
            // delete message
            println("message ${context.messageId} in chat ${context.chatId} deleted!")
        }
    }
}

suspend inline fun WorkflowsWorker.executeExampleWorkflow(context: ExampleWorkflowContext) =
    execute<ExampleWorkflow, ExampleWorkflowContext>(ExampleWorkflow.getId(context), context)
