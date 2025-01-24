package ru.killwolfvlad.workflows.activities

import kotlinx.coroutines.withContext
import ru.killwolfvlad.workflows.activities.coroutines.ActivityCoroutineContextElement
import ru.killwolfvlad.workflows.activities.enums.ActivityStatus

const val STATUS_ACTIVITY_CONTEXT_KEY = "status"

suspend fun withActivity(
    activityContext: ActivityContext,
    activityId: String,
    block: suspend () -> Map<String, String>?,
) = withContext(ActivityCoroutineContextElement(activityId)) activity@{
    // TODO: check for cancel
    val status = activityContext.get(STATUS_ACTIVITY_CONTEXT_KEY)?.let {
        ActivityStatus.valueOf(it)
    }

    if (status == ActivityStatus.COMPLETED) {
        return@activity
    }

    val workflowContext = block()

    activityContext.set(
        mapOf(STATUS_ACTIVITY_CONTEXT_KEY to ActivityStatus.COMPLETED.toString()),
        workflowContext ?: emptyMap()
    )
}
