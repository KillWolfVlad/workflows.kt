package ru.killwolfvlad.workflows.coroutines

import ru.killwolfvlad.workflows.types.WorkflowId
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@ConsistentCopyVisibility
data class WorkflowCoroutineContextElement internal constructor(
    val workflowId: WorkflowId,
    internal val workflowKey: String,
) : AbstractCoroutineContextElement(WorkflowCoroutineContextElement) {
    companion object Key : CoroutineContext.Key<WorkflowCoroutineContextElement>
}

suspend inline fun CoroutineContext.getWorkflowId(): WorkflowId =
    coroutineContext[WorkflowCoroutineContextElement]?.workflowId
        ?: throw NullPointerException("${WorkflowCoroutineContextElement::class.simpleName} must be in coroutineContext!")

internal suspend inline fun CoroutineContext.getWorkflowKey(): String =
    coroutineContext[WorkflowCoroutineContextElement]?.workflowKey
        ?: throw NullPointerException("${WorkflowCoroutineContextElement::class.simpleName} must be in coroutineContext!")
