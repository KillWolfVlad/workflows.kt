package ru.killwolfvlad.workflows.core.internal

import io.ktor.util.collections.*
import kotlinx.coroutines.*
import ru.killwolfvlad.workflows.core.ActivityContext
import ru.killwolfvlad.workflows.core.WorkflowContext
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
    rootJob: Job,
    private val activityContext: ActivityContext,
    private val config: WorkflowsConfig,
    private val keyValueClient: KeyValueClient,
    private val workflowContext: WorkflowContext,
    private val workflowsClassManager: WorkflowsClassManager,
    private val workflowsExceptionHandler: WorkflowsExceptionHandler,
) {
    private val coroutineScope = CoroutineScope(
        rootJob + Dispatchers.IO + CoroutineName(WorkflowsRunner::class.simpleName + "Coroutine"),
    )

    private val workflowJobs = ConcurrentMap<WorkflowId, Job>()

    suspend fun run(
        workflowId: WorkflowId,
        initialContext: Map<String, String>,
        workflowClass: KClass<out Workflow>,
    ) {
        if (contains(workflowId)) {
            return
        }

        val workflowKey = workflowId.workflowKey

        if (!acquireWorkflowLock(workflowId, workflowKey, initialContext, workflowClass)) {
            return
        }

        launchWorkflow(workflowId, workflowKey, workflowClass)
    }

    fun cancel(workflowId: WorkflowId) {
        workflowJobs[workflowId]?.cancel()
    }

    fun contains(workflowId: WorkflowId): Boolean =
        workflowJobs.containsKey(workflowId)

    private suspend inline fun acquireWorkflowLock(
        workflowId: WorkflowId,
        workflowKey: String,
        initialContext: Map<String, String>,
        workflowClass: KClass<out Workflow>,
    ): Boolean =
        keyValueClient.acquireWorkflowLock(
            // keys
            workflowKey = workflowKey,
            workflowLocksKey = WORKFLOW_LOCKS_KEY,
            workflowWorkersKey = WORKFLOW_WORKERS_KEY,
            // arguments
            workflowId = workflowId,
            workerId = config.workerId,
            // workflow context
            workflowClassNameFieldKey = WORKFLOW_CLASS_NAME_FIELD_KEY,
            workflowClassName = workflowClass.workflowClassName,
            initialContext = initialContext,
        ) >= 1L

    private fun launchWorkflow(
        workflowId: WorkflowId,
        workflowKey: String,
        workflowClass: KClass<out Workflow>,
    ) {
        workflowJobs[workflowId] = coroutineScope.launch(
            WorkflowCoroutineContextElement(workflowId, workflowContext, activityContext, workflowKey, keyValueClient),
            CoroutineStart.LAZY,
        ) workflow@{
            try {
                val signal = keyValueClient.hGet(workflowKey, WORKFLOW_SIGNAL_FIELD_KEY)

                if (signal != WORKFLOW_CANCEL_SIGNAL) {
                    val workflow = workflowsClassManager.getInstance(workflowClass)

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
                    workflowLocksKey = WORKFLOW_LOCKS_KEY,
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
                workflowJobs.remove(workflowId)
            }

            it.start()
        }
    }
}
