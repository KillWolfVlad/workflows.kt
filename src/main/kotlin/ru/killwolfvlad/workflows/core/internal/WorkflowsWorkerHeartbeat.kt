package ru.killwolfvlad.workflows.core.internal

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.interfaces.runSafe
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_WORKERS_KEY

@OptIn(WorkflowsPerformance::class)
internal class WorkflowsWorkerHeartbeat(
    rootJob: Job,
    private val config: WorkflowsConfig,
    private val keyValueClient: KeyValueClient,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
    private val workflowsRunner: WorkflowsRunner,
) {
    private val coroutineScope =
        CoroutineScope(
            rootJob + Dispatchers.IO + CoroutineName(WorkflowsWorkerHeartbeat::class.simpleName + "Coroutine"),
        )

    suspend fun init() {
        heartbeat()

        coroutineScope.launch {
            while (true) {
                delay(config.heartbeatInterval)

                workflowsExceptionHandler.runSafe {
                    heartbeat()
                }
            }
        }
    }

    private suspend inline fun heartbeat() =
        keyValueClient.heartbeat(WORKFLOW_WORKERS_KEY, config.workerId, workflowsRunner.activeWorkflows, config.lockTimeout)
}
