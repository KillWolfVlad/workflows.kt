package ru.killwolfvlad.workflows.core.internal

import kotlinx.serialization.json.Json
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_CANCEL_SIGNAL
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_SIGNAL_CHANNEL
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_SIGNAL_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.dtos.WorkflowSignalMessageDto
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey

internal class WorkflowSignalsBroker(
    private val keyValueClient: KeyValueClient,
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
            val message = json.decodeFromString<WorkflowSignalMessageDto>(messageString)

            when (message.signal) {
                WORKFLOW_CANCEL_SIGNAL -> {
                    workflowsRunner.cancel(message.workflowId)
                }

                else -> NotImplementedError("unknown signal ${message.signal}!")
            }
        }
    }

    private suspend fun set(workflowId: WorkflowId, signal: String) {
        keyValueClient.hSetIfKeyExists(workflowId.workflowKey, WORKFLOW_SIGNAL_FIELD_KEY to signal)
    }

    private suspend fun publish(workflowId: WorkflowId, signal: String) {
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
