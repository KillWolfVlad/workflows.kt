package ru.killwolfvlad.workflows.core.internal

import kotlinx.coroutines.*
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_WORKERS_KEY

class WorkflowsWorkerHeartbeat(
    mainJob: Job,
    private val config: WorkflowsConfig,
    private val keyValueClient: KeyValueClient,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
) {
    private val coroutineScope = CoroutineScope(
        Dispatchers.IO + CoroutineName(WorkflowsWorkerHeartbeat::class.simpleName + "Coroutine") + mainJob,
    )

    suspend fun init() {
        keyValueClient.heartbeat(WORKFLOW_WORKERS_KEY, config.workerId, config.lockTimeout)

        coroutineScope.launch {
            while (true) {
                delay(config.heartbeatInterval)

                try {
                    keyValueClient.heartbeat(WORKFLOW_WORKERS_KEY, config.workerId, config.lockTimeout)
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
}
