package ru.killwolfvlad.workflows

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.killwolfvlad.workflows.consts.WORKFLOW_CANCEL_SIGNAL
import ru.killwolfvlad.workflows.consts.WORKFLOW_SIGNAL_CHANNEL
import ru.killwolfvlad.workflows.consts.WORKFLOW_SIGNAL_FIELD_KEY
import ru.killwolfvlad.workflows.dtos.WorkflowSignalMessageDto
import ru.killwolfvlad.workflows.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.types.WorkflowId
import ru.killwolfvlad.workflows.types.workflowKey

internal class WorkflowSignalsBroker(
    private val keyValueClient: KeyValueClient,
    private val workflowsRunner: WorkflowsRunner,
) {
    private inline val json: Json
        get() = Json

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
