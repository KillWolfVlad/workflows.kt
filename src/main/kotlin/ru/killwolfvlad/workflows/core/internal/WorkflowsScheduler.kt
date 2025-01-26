package ru.killwolfvlad.workflows.core.internal

import kotlinx.coroutines.*
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_CLASS_NAME_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_LOCKS_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_WORKERS_KEY
import ru.killwolfvlad.workflows.core.internal.extensions.workflowClass
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey

@OptIn(WorkflowsPerformance::class)
internal class WorkflowsScheduler(
    rootJob: Job,
    private val config: WorkflowsConfig,
    private val keyValueClient: KeyValueClient,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
    private val workflowsRunner: WorkflowsRunner,
) {
    private val coroutineScope = CoroutineScope(
        rootJob + Dispatchers.IO + CoroutineName(WorkflowsScheduler::class.simpleName + "Coroutine"),
    )

    private lateinit var fetchJob: Job

    suspend fun init() {
        fetchWorkflows()

        fetchJob = coroutineScope.launch {
            delay(config.fetchInterval)

            while (true) {
                try {
                    fetchWorkflows()
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

    private suspend inline fun fetchWorkflows() {
        val (locks, workers) = keyValueClient.pipelineHGetAll(WORKFLOW_LOCKS_KEY, WORKFLOW_WORKERS_KEY)

        val workflowsToRun = locks.filter filterWorkflows@{
            if (workflowsRunner.contains(WorkflowId(it.key))) {
                return@filterWorkflows false
            }

            if (it.value == config.workerId) {
                return@filterWorkflows true
            }

            return@filterWorkflows !workers.contains(it.value)
        }.map {
            object {
                val workflowId = WorkflowId(it.key)
                val workflowKey = workflowId.workflowKey
            }
        }

        if (workflowsToRun.isEmpty()) {
            return
        }

        val runPayloads =
            keyValueClient.pipelineHGet(
                *workflowsToRun.map { it.workflowKey to WORKFLOW_CLASS_NAME_FIELD_KEY }.toTypedArray()
            ).zip(workflowsToRun) { a, b ->
                object {
                    val workflowId = b.workflowId
                    val workflowClass = a?.workflowClass
                }
            }

        runPayloads.map { runPayload ->
            coroutineScope.async run@{
                if (runPayload.workflowClass == null) {
                    return@run
                }

                workflowsRunner.run(runPayload.workflowId, emptyMap(), runPayload.workflowClass)
            }
        }.awaitAll()
    }
}
