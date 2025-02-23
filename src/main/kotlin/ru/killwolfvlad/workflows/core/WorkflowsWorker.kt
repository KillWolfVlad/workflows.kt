package ru.killwolfvlad.workflows.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToStringMap
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

@OptIn(WorkflowsPerformance::class, ExperimentalSerializationApi::class)
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
        WorkflowsWorkerHeartbeat(config.rootJob, config, keyValueClient, workflowsExceptionHandler, workflowsRunner)

    private val workflowsSignalsBroker =
        WorkflowsSignalsBroker(keyValueClient, workflowsExceptionHandler, workflowsRunner)

    private val workflowsScheduler =
        WorkflowsScheduler(config.rootJob, config, keyValueClient, workflowsExceptionHandler, workflowsRunner)

    val activeWorkflows: Int
        get() = workflowsRunner.activeWorkflows

    suspend fun execute(
        workflowId: WorkflowId,
        initialContext: Map<String, String>,
        workflowClass: KClass<out Workflow>,
    ) = workflowsRunner.run(workflowId, initialContext, workflowClass)

    suspend inline fun <reified T : Workflow> execute(
        workflowId: WorkflowId,
        initialContext: Map<String, String>,
    ) = execute(workflowId, initialContext, T::class)

    suspend inline fun <reified TWorkflow : Workflow, reified TInitialContext> execute(
        workflowId: WorkflowId,
        initialContext: TInitialContext,
    ) = execute<TWorkflow>(workflowId, Properties.encodeToStringMap(initialContext))

    suspend inline fun <reified TWorkflow : Workflow, reified TInitialContext1, reified TInitialContext2> execute(
        workflowId: WorkflowId,
        initialContext1: TInitialContext1,
        initialContext2: TInitialContext2,
    ) = execute<TWorkflow>(workflowId, Properties.encodeToStringMap(initialContext1) + Properties.encodeToStringMap(initialContext2))

    suspend fun cancel(workflowId: WorkflowId) = workflowsSignalsBroker.cancel(workflowId)

    suspend fun init() {
        workflowsWorkerHeartbeat.init()
        workflowsSignalsBroker.init()
        workflowsScheduler.init()
    }
}
