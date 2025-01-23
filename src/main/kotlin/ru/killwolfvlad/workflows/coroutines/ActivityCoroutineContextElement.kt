package ru.killwolfvlad.workflows.coroutines

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

data class ActivityCoroutineContextElement(
    val activityId: String,
) : AbstractCoroutineContextElement(ActivityCoroutineContextElement) {
    companion object Key : CoroutineContext.Key<ActivityCoroutineContextElement>
}

suspend inline fun CoroutineContext.getActivityId(): String =
    coroutineContext[ActivityCoroutineContextElement]?.activityId
        ?: throw NullPointerException("${ActivityCoroutineContextElement::class.simpleName} must be in coroutineContext!")
