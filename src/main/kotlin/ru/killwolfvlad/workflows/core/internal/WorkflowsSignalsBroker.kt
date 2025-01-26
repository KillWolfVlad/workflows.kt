package ru.killwolfvlad.workflows.core.internal

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.dtos.WorkflowSignalMessageDto
import ru.killwolfvlad.workflows.core.internal.enums.WorkflowSignal
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey

@OptIn(WorkflowsPerformance::class)
internal class WorkflowsSignalsBroker(
    private val keyValueClient: KeyValueClient,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
    private val workflowsRunner: WorkflowsRunner,
) {
    private inline val json: Json
        get() = Json.Default

    suspend fun cancel(workflowId: WorkflowId) {
        set(workflowId, WorkflowSignal.CANCEL)

        if (workflowsRunner.contains(workflowId)) {
            workflowsRunner.cancel(workflowId)
        } else {
            publish(workflowId, WorkflowSignal.CANCEL)
        }
    }

    suspend fun init() {
        keyValueClient.subscribe(WorkflowSignal.CHANNEL) { messageString ->
            try {
                val message = json.decodeFromString<WorkflowSignalMessageDto>(messageString)

                when (message.signal) {
                    WorkflowSignal.CANCEL -> {
                        workflowsRunner.cancel(message.workflowId)
                    }
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

    private suspend inline fun set(workflowId: WorkflowId, signal: WorkflowSignal) {
        keyValueClient.hSetIfKeyExistsScript(workflowId.workflowKey, WorkflowSignal.FIELD_KEY to signal.toString())
    }

    private suspend inline fun publish(workflowId: WorkflowId, signal: WorkflowSignal) {
        keyValueClient.publish(
            WorkflowSignal.CHANNEL, json.encodeToString(
                WorkflowSignalMessageDto(
                    workflowId = workflowId,
                    signal = signal,
                )
            )
        )
    }
}
