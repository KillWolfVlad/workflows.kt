package ru.killwolfvlad.workflows.core.coroutines

import ru.killwolfvlad.workflows.core.interfaces.Workflow
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

@ConsistentCopyVisibility
data class ActivityCoroutineContextElement internal constructor(
    val activityId: String,
) : AbstractCoroutineContextElement(ActivityCoroutineContextElement) {
    companion object Key : CoroutineContext.Key<ActivityCoroutineContextElement>
}

suspend inline fun CoroutineContext.getActivityId(): String =
    coroutineContext[ActivityCoroutineContextElement]?.activityId
        ?: throw NullPointerException("${ActivityCoroutineContextElement::class.simpleName} must be in coroutineContext!")

suspend inline fun Workflow.getActivityId(): String =
    coroutineContext.getActivityId()
