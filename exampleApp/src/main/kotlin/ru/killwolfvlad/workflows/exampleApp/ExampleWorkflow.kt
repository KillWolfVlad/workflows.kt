package ru.killwolfvlad.workflows.exampleApp

import kotlinx.serialization.Serializable
import ru.killwolfvlad.workflows.activities.delayActivity
import ru.killwolfvlad.workflows.core.WorkflowsWorker
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.withActivity
import ru.killwolfvlad.workflows.core.withWorkflowContext
import kotlin.time.Duration

class ExampleWorkflow : Workflow {
    @Serializable
    data class DelayContext(
        val duration: Duration,
    )

    @Serializable
    data class DeleteMessageContext(
        val chatId: Long,
        val messageId: Long,
    )

    companion object {
        fun getId(deleteMessageContext: DeleteMessageContext): WorkflowId =
            WorkflowId("exampleWorkflow:${deleteMessageContext.chatId}:${deleteMessageContext.messageId}")
    }

    override suspend fun execute() {
        withWorkflowContext<DelayContext> { delayContext ->
            delayActivity("delay", delayContext.duration)
        }

        withActivity<DeleteMessageContext, Unit>("deleteMessage") { deleteMessageContext ->
            // delete message
            println("message ${deleteMessageContext.messageId} in chat ${deleteMessageContext.chatId} deleted!")
        }
    }
}

suspend inline fun WorkflowsWorker.executeExampleWorkflow(
    delayContext: ExampleWorkflow.DelayContext,
    deleteMessageContext: ExampleWorkflow.DeleteMessageContext,
) = execute<ExampleWorkflow, ExampleWorkflow.DelayContext, ExampleWorkflow.DeleteMessageContext>(
    ExampleWorkflow.getId(deleteMessageContext),
    delayContext,
    deleteMessageContext,
)
