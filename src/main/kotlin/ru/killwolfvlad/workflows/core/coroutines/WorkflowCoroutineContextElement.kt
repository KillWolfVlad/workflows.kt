package ru.killwolfvlad.workflows.core.coroutines

import ru.killwolfvlad.workflows.core.ActivityContext
import ru.killwolfvlad.workflows.core.WorkflowContext
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.types.WorkflowId
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@ConsistentCopyVisibility
data class WorkflowCoroutineContextElement internal constructor(
    val workflowId: WorkflowId,
    val workflowContext: WorkflowContext,
    val activityContext: ActivityContext,
    internal val workflowKey: String,
    internal val keyValueClient: KeyValueClient,
) : AbstractCoroutineContextElement(WorkflowCoroutineContextElement) {
    companion object Key : CoroutineContext.Key<WorkflowCoroutineContextElement>
}

suspend inline fun CoroutineContext.getWorkflowId(): WorkflowId =
    coroutineContext[WorkflowCoroutineContextElement]?.workflowId
        ?: throw NullPointerException("${WorkflowCoroutineContextElement::class.simpleName} must be in coroutineContext!")

suspend inline fun CoroutineContext.getWorkflowContext(): WorkflowContext =
    coroutineContext[WorkflowCoroutineContextElement]?.workflowContext
        ?: throw NullPointerException("${WorkflowCoroutineContextElement::class.simpleName} must be in coroutineContext!")

suspend inline fun CoroutineContext.getActivityContext(): ActivityContext =
    coroutineContext[WorkflowCoroutineContextElement]?.activityContext
        ?: throw NullPointerException("${WorkflowCoroutineContextElement::class.simpleName} must be in coroutineContext!")

internal suspend inline fun CoroutineContext.getWorkflowKey(): String =
    coroutineContext[WorkflowCoroutineContextElement]?.workflowKey
        ?: throw NullPointerException("${WorkflowCoroutineContextElement::class.simpleName} must be in coroutineContext!")

internal suspend inline fun CoroutineContext.getKeyValueClient(): KeyValueClient =
    coroutineContext[WorkflowCoroutineContextElement]?.keyValueClient
        ?: throw NullPointerException("${WorkflowCoroutineContextElement::class.simpleName} must be in coroutineContext!")

suspend inline fun Workflow.getWorkflowId(): WorkflowId =
    coroutineContext.getWorkflowId()

suspend inline fun Workflow.getWorkflowContext(): WorkflowContext =
    coroutineContext.getWorkflowContext()

suspend inline fun Workflow.getActivityContext(): ActivityContext =
    coroutineContext.getActivityContext()
