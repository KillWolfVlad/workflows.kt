package ru.killwolfvlad.workflows.core.coroutines

import ru.killwolfvlad.workflows.core.ActivityContext
import ru.killwolfvlad.workflows.core.WorkflowContext
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.types.WorkflowId
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@OptIn(WorkflowsPerformance::class)
@ConsistentCopyVisibility
data class WorkflowCoroutineContext internal constructor(
    val workflowId: WorkflowId,
    val workflowContext: WorkflowContext,
    val activityContext: ActivityContext,
    internal val workflowKey: String, // TODO: remove workflowKey from context
    internal val keyValueClient: KeyValueClient,
) : AbstractCoroutineContextElement(WorkflowCoroutineContext) {
    companion object Key : CoroutineContext.Key<WorkflowCoroutineContext>
}

suspend inline fun CoroutineContext.getWorkflowId(): WorkflowId =
    coroutineContext[WorkflowCoroutineContext]?.workflowId
        ?: throw NullPointerException("${WorkflowCoroutineContext::class.simpleName} must be in coroutineContext!")

@OptIn(WorkflowsPerformance::class)
suspend inline fun CoroutineContext.getWorkflowContext(): WorkflowContext =
    coroutineContext[WorkflowCoroutineContext]?.workflowContext
        ?: throw NullPointerException("${WorkflowCoroutineContext::class.simpleName} must be in coroutineContext!")

@OptIn(WorkflowsPerformance::class)
suspend inline fun CoroutineContext.getActivityContext(): ActivityContext =
    coroutineContext[WorkflowCoroutineContext]?.activityContext
        ?: throw NullPointerException("${WorkflowCoroutineContext::class.simpleName} must be in coroutineContext!")

internal suspend inline fun CoroutineContext.getWorkflowKey(): String =
    coroutineContext[WorkflowCoroutineContext]?.workflowKey
        ?: throw NullPointerException("${WorkflowCoroutineContext::class.simpleName} must be in coroutineContext!")

@OptIn(WorkflowsPerformance::class)
internal suspend inline fun CoroutineContext.getKeyValueClient(): KeyValueClient =
    coroutineContext[WorkflowCoroutineContext]?.keyValueClient
        ?: throw NullPointerException("${WorkflowCoroutineContext::class.simpleName} must be in coroutineContext!")

suspend inline fun Workflow.getWorkflowId(): WorkflowId =
    coroutineContext.getWorkflowId()

@OptIn(WorkflowsPerformance::class)
suspend inline fun Workflow.getWorkflowContext(): WorkflowContext =
    coroutineContext.getWorkflowContext()

@OptIn(WorkflowsPerformance::class)
suspend inline fun Workflow.getActivityContext(): ActivityContext =
    coroutineContext.getActivityContext()
