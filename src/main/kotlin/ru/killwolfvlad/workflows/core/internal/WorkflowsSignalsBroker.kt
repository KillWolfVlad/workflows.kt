package ru.killwolfvlad.workflows.core.internal

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_CANCEL_SIGNAL
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_SIGNAL_CHANNEL
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_SIGNAL_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.dtos.WorkflowSignalMessageDto
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey

internal class WorkflowsSignalsBroker(
    private val keyValueClient: KeyValueClient,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
    private val workflowsRunner: WorkflowsRunner,
) {
    private inline val json: Json
        get() = Json.Default

    suspend fun cancel(workflowId: WorkflowId) {
        set(workflowId, WORKFLOW_CANCEL_SIGNAL)

        if (workflowsRunner.contains(workflowId)) {
            workflowsRunner.cancel(workflowId)
        } else {
            publish(workflowId, WORKFLOW_CANCEL_SIGNAL)
        }
    }

    suspend fun init() {
        keyValueClient.subscribe(WORKFLOW_SIGNAL_CHANNEL) { messageString ->
            try {
                val message = json.decodeFromString<WorkflowSignalMessageDto>(messageString)

                when (message.signal) {
                    WORKFLOW_CANCEL_SIGNAL -> {
                        workflowsRunner.cancel(message.workflowId)
                    }

                    else -> throw NotImplementedError("unknown signal ${message.signal}!")
                }
            } catch (_: CancellationException) {
                // skip cancellation exception
            } catch (exception: Exception) {
                runCatching {
                    workflowsExceptionHandler.handle(exception)
                }
            }
        }
    }

    private suspend inline fun set(workflowId: WorkflowId, signal: String) {
        keyValueClient.hSetIfKeyExistsScript(workflowId.workflowKey, WORKFLOW_SIGNAL_FIELD_KEY to signal)
    }

    private suspend inline fun publish(workflowId: WorkflowId, signal: String) {
        keyValueClient.publish(
            WORKFLOW_SIGNAL_CHANNEL, json.encodeToString(
                WorkflowSignalMessageDto(
                    workflowId = workflowId,
                    signal = signal,
                )
            )
        )
    }
}
