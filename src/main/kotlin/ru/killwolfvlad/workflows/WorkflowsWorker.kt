package ru.killwolfvlad.workflows

import ru.killwolfvlad.workflows.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.interfaces.Workflow
import ru.killwolfvlad.workflows.interfaces.WorkflowsClassManager
import ru.killwolfvlad.workflows.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.types.WorkflowId
import kotlin.reflect.KClass

class WorkflowsWorker(
    config: WorkflowsConfig,
    keyValueClient: KeyValueClient,
    workflowsClassManager: WorkflowsClassManager,
    workflowsExceptionHandler: WorkflowsExceptionHandler,
) {
    private val workflowsRunner =
        WorkflowsRunner(config, keyValueClient, workflowsClassManager, workflowsExceptionHandler)

    private val workflowSignalsBroker = WorkflowSignalsBroker(keyValueClient, workflowsRunner)

    private val workflowsScheduler =
        WorkflowsScheduler(config, keyValueClient, workflowsExceptionHandler, workflowsRunner)

    suspend fun run(
        workflowId: WorkflowId,
        initialContext: Map<String, String>,
        workflowClass: KClass<out Workflow>,
    ) = workflowsRunner.run(workflowId, initialContext, workflowClass)

    suspend fun cancel(workflowId: WorkflowId) =
        workflowSignalsBroker.cancel(workflowId)

    suspend fun init() {
        workflowSignalsBroker.init()
        workflowsScheduler.init()
    }
}

suspend inline fun <reified T : Workflow> WorkflowsWorker.run(
    workflowId: WorkflowId,
    initialContext: Map<String, String>,
) = run(workflowId, initialContext, T::class)
