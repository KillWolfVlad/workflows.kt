package ru.killwolfvlad.workflows.core

import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsClassManager
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.WorkflowsRunner
import ru.killwolfvlad.workflows.core.internal.WorkflowsScheduler
import ru.killwolfvlad.workflows.core.internal.WorkflowsSignalsBroker
import ru.killwolfvlad.workflows.core.internal.WorkflowsWorkerHeartbeat
import ru.killwolfvlad.workflows.core.types.WorkflowId
import kotlin.reflect.KClass

@OptIn(WorkflowsPerformance::class)
class WorkflowsWorker(
    config: WorkflowsConfig,
    keyValueClient: KeyValueClient,
    workflowsClassManager: WorkflowsClassManager,
    workflowsExceptionHandler: WorkflowsExceptionHandler,
) {
    private val workflowContext = WorkflowContext(keyValueClient)

    private val activityContext = ActivityContext(keyValueClient)

    private val workflowsRunner =
        WorkflowsRunner(
            config.rootJob,
            activityContext,
            config,
            keyValueClient,
            workflowContext,
            workflowsClassManager,
            workflowsExceptionHandler,
        )

    private val workflowsWorkerHeartbeat =
        WorkflowsWorkerHeartbeat(config.rootJob, config, keyValueClient, workflowsExceptionHandler)

    private val workflowsSignalsBroker =
        WorkflowsSignalsBroker(keyValueClient, workflowsExceptionHandler, workflowsRunner)

    private val workflowsScheduler =
        WorkflowsScheduler(config.rootJob, config, keyValueClient, workflowsExceptionHandler, workflowsRunner)

    suspend fun run(
        workflowId: WorkflowId,
        initialContext: Map<String, String>,
        workflowClass: KClass<out Workflow>,
    ) = workflowsRunner.run(workflowId, initialContext, workflowClass)

    suspend fun cancel(workflowId: WorkflowId) = workflowsSignalsBroker.cancel(workflowId)

    suspend fun init() {
        workflowsWorkerHeartbeat.init()
        workflowsSignalsBroker.init()
        workflowsScheduler.init()
    }
}

suspend inline fun <reified T : Workflow> WorkflowsWorker.run(
    workflowId: WorkflowId,
    initialContext: Map<String, String>,
) = run(workflowId, initialContext, T::class)
