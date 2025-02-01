package ru.killwolfvlad.workflows.activities

import ru.killwolfvlad.workflows.activities.types.ActivityCallback
import ru.killwolfvlad.workflows.core.withActivity

suspend fun withActivity(
    activityId: String,
    workflowContextKeys: List<String> = emptyList(),
    block: ActivityCallback,
) = withActivity(activityId, workflowContextKeys = workflowContextKeys) { workflowContextMap, _ ->
    block(workflowContextMap)
}
