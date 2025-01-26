package ru.killwolfvlad.workflows.core.internal

import kotlinx.coroutines.*
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_WORKERS_KEY

class WorkflowsWorkerHeartbeat(
    rootJob: Job,
    private val config: WorkflowsConfig,
    private val keyValueClient: KeyValueClient,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
) {
    private val coroutineScope = CoroutineScope(
        rootJob + Dispatchers.IO + CoroutineName(WorkflowsWorkerHeartbeat::class.simpleName + "Coroutine"),
    )

    suspend fun init() {
        heartbeat()

        coroutineScope.launch {
            while (true) {
                delay(config.heartbeatInterval)

                try {
                    heartbeat()
                } catch (_: CancellationException) {
                    // skip cancellation exception
                } catch (exception: Exception) {
                    runCatching {
                        workflowsExceptionHandler.handle(exception)
                    }
                }
            }
        }
    }

    private suspend inline fun heartbeat() =
        keyValueClient.heartbeat(WORKFLOW_WORKERS_KEY, config.workerId, config.lockTimeout)
}
