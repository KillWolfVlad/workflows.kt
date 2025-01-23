package ru.killwolfvlad.workflows.core.internal

import kotlinx.coroutines.*
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_CLASS_NAME_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_IDS_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_LOCK_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.extensions.workflowClass
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey

internal class WorkflowsScheduler(
    private val config: WorkflowsConfig,
    private val keyValueClient: KeyValueClient,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
    private val workflowsRunner: WorkflowsRunner,
) {
    private val coroutineScope = CoroutineScope(
        Dispatchers.IO + CoroutineName(WorkflowsScheduler::class.simpleName + "Coroutine"),
    )

    private lateinit var fetchJob: Job

    fun init() {
        fetchJob = coroutineScope.launch {
            do {
                try {
                    fetchWorkflows()
                } catch (_: CancellationException) {
                    // skip cancellation exception
                } catch (exception: Exception) {
                    runCatching {
                        workflowsExceptionHandler.handle(exception)
                    }
                }

                delay(config.fetchInterval)
            } while (true)
        }
    }

    private suspend fun fetchWorkflows() {
        val workflows = keyValueClient.sMembers(WORKFLOW_IDS_KEY).map {
            object {
                val workflowId = WorkflowId(it)
                val workflowKey = workflowId.workflowKey
            }
        }.filter {
            !workflowsRunner.contains(it.workflowId)
        }

        if (workflows.isEmpty()) {
            return
        }

        val workflowLocks = keyValueClient.pipelineHGet(
            *workflows.map { it.workflowKey to WORKFLOW_LOCK_FIELD_KEY }.toTypedArray()
        ).zip(workflows) { a, b ->
            object {
                val workflowId = b.workflowId
                val workflowKey = b.workflowKey
                val lockWorkerId = a
            }
        }

        val workflowsToRun = workflowLocks.filter {
            it.lockWorkerId == config.workerId || it.lockWorkerId == null
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
