package ru.killwolfvlad.workflows.core.coroutines

import ru.killwolfvlad.workflows.core.interfaces.Workflow
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

data class ActivityCoroutineContext(
    val activityId: String,
) : AbstractCoroutineContextElement(ActivityCoroutineContext) {
    companion object Key : CoroutineContext.Key<ActivityCoroutineContext>
}

suspend inline fun CoroutineContext.getActivityId(): String =
    coroutineContext[ActivityCoroutineContext]?.activityId
        ?: throw NullPointerException("${ActivityCoroutineContext::class.simpleName} must be in coroutineContext!")

suspend inline fun Workflow.getActivityId(): String = coroutineContext.getActivityId()
