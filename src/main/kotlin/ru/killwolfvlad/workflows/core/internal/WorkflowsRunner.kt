package ru.killwolfvlad.workflows.core.internal

import io.ktor.util.collections.*
import kotlinx.coroutines.*
import ru.killwolfvlad.workflows.core.WorkflowsConfig
import ru.killwolfvlad.workflows.core.coroutines.WorkflowCoroutineContextElement
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsClassManager
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.consts.*
import ru.killwolfvlad.workflows.core.internal.extensions.workflowClassName
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey
import kotlin.reflect.KClass

internal class WorkflowsRunner(
    private val config: WorkflowsConfig,
    private val keyValueClient: KeyValueClient,
    private val workflowsClassManager: WorkflowsClassManager,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
) {
    private val coroutineScope = CoroutineScope(
        Dispatchers.IO + CoroutineName(WorkflowsRunner::class.simpleName + "Coroutine"),
    )

    private val workflowInstancesMap = ConcurrentMap<WorkflowId, Job>()
    private val workflowHeartbeatsMap = ConcurrentMap<WorkflowId, Job>()

    suspend fun run(
        workflowId: WorkflowId,
        initialContext: Map<String, String>,
        workflowClass: KClass<out Workflow>,
    ) {
        if (contains(workflowId)) {
            return
        }

        val workflowKey = workflowId.workflowKey

        if (!acquireLock(workflowId, workflowKey, initialContext, workflowClass)) {
            return
        }

        launchHeartbeat(workflowId, workflowKey)

        launchWorkflow(workflowId, workflowKey, workflowClass)
    }

    fun cancel(workflowId: WorkflowId) {
        workflowInstancesMap[workflowId]?.cancel()
    }

    fun contains(workflowId: WorkflowId): Boolean =
        workflowInstancesMap.containsKey(workflowId) || workflowHeartbeatsMap.containsKey(workflowId)

    private suspend fun acquireLock(
        workflowId: WorkflowId,
        workflowKey: String,
        initialContext: Map<String, String>,
        workflowClass: KClass<out Workflow>,
    ): Boolean =
        keyValueClient.acquireLock(
            // keys
            workflowKey = workflowKey,
            workflowIdsKey = WORKFLOW_IDS_KEY,
            // arguments
            workflowId = workflowId,
            lockTimeout = config.lockTimeout,
            // workflow context
            workflowLockFieldKey = WORKFLOW_LOCK_FIELD_KEY,
            workerId = config.workerId,
            workflowClassNameFieldKey = WORKFLOW_CLASS_NAME_FIELD_KEY,
            workflowClassName = workflowClass.workflowClassName,
            initialContext = initialContext,
        )

    private fun launchHeartbeat(
        workflowId: WorkflowId,
        workflowKey: String,
    ) {
        workflowHeartbeatsMap[workflowId] = coroutineScope.launch(start = CoroutineStart.LAZY) heartbeat@{
            while (true) {
                delay(config.heartbeatInterval)

                if (workflowInstancesMap[workflowId] == null) {
                    return@heartbeat
                }

                try {
                    val signal = keyValueClient.heartbeat(
                        // keys
                        workflowKey = workflowKey,
                        // arguments
                        lockTimeout = config.lockTimeout,
                        workflowLockFieldKey = WORKFLOW_LOCK_FIELD_KEY,
                        workflowSignalFieldKey = WORKFLOW_SIGNAL_FIELD_KEY
                    )

                    if (signal == WORKFLOW_CANCEL_SIGNAL) {
                        workflowInstancesMap[workflowId]?.cancel()
                    }
                } catch (_: CancellationException) {
                    return@heartbeat
                } catch (exception: Exception) {
                    runCatching {
                        workflowsExceptionHandler.handle(exception)
                    }
                }
            }
        }.also {
            it.invokeOnCompletion {
                workflowHeartbeatsMap.remove(workflowId)
            }

            it.start()
        }
    }

    private fun launchWorkflow(
        workflowId: WorkflowId,
        workflowKey: String,
        workflowClass: KClass<out Workflow>,
    ) {
        val workflow = workflowsClassManager.getInstance(workflowClass)

        workflowInstancesMap[workflowId] = coroutineScope.launch(
            WorkflowCoroutineContextElement(workflowId, workflowKey),
            CoroutineStart.LAZY,
        ) workflow@{
            try {
                val signal = keyValueClient.hGet(workflowKey, WORKFLOW_SIGNAL_FIELD_KEY)

                if (signal != WORKFLOW_CANCEL_SIGNAL) {
                    workflow.execute()
                }
            } catch (_: CancellationException) {
                // skip cancellation exception
            } catch (exception: Exception) {
                runCatching {
                    workflowsExceptionHandler.handle(exception)
                }

                return@workflow
            }

            try {
                keyValueClient.deleteWorkflow(
                    // keys
                    workflowKey = workflowKey,
                    workflowIdsKey = WORKFLOW_IDS_KEY,
                    // arguments
                    workflowId = workflowId,
                )
            } catch (_: CancellationException) {
                // skip cancellation exception
            } catch (exception: Exception) {
                runCatching {
                    workflowsExceptionHandler.handle(exception)
                }
            }
        }.also {
            it.invokeOnCompletion {
                workflowInstancesMap.remove(workflowId)
                workflowHeartbeatsMap[workflowId]?.cancel()
            }

            it.start()
        }
    }
}
