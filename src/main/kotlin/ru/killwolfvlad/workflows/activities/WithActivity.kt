package ru.killwolfvlad.workflows.activities

import ru.killwolfvlad.workflows.core.withActivity

suspend fun withActivity(
    activityId: String,
    workflowContextKeys: List<String> = emptyList(),
    block: suspend (workflowContextMap: Map<String, String?>) -> Map<String, String>?,
) = withActivity(activityId, workflowContextKeys = workflowContextKeys) { workflowContextMap, _ ->
    block(workflowContextMap)
}
