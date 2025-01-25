package ru.killwolfvlad.workflows.core

import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import ru.killwolfvlad.workflows.core.coroutines.ActivityCoroutineContextElement
import ru.killwolfvlad.workflows.core.coroutines.getActivityId
import ru.killwolfvlad.workflows.core.coroutines.getKeyValueClient
import ru.killwolfvlad.workflows.core.coroutines.getWorkflowKey
import ru.killwolfvlad.workflows.core.internal.consts.ACTIVITY_FIELD_KEY_PREFIX
import ru.killwolfvlad.workflows.core.internal.consts.ACTIVITY_STATUS_FIELD_NAME
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_CANCEL_SIGNAL
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_SIGNAL_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.enums.ActivityStatus

suspend fun withActivity(
    activityId: String,
    block: suspend () -> Map<String, String>?,
) = withContext(ActivityCoroutineContextElement(activityId)) activity@{
    val workflowKey = coroutineContext.getWorkflowKey()
    val keyValueClient = coroutineContext.getKeyValueClient()

    val activityStatusFieldKey =
        "${ACTIVITY_FIELD_KEY_PREFIX}:${kotlin.coroutines.coroutineContext.getActivityId()}:${ACTIVITY_STATUS_FIELD_NAME}"

    val (signal, status) = keyValueClient.hMGet(workflowKey, WORKFLOW_SIGNAL_FIELD_KEY, activityStatusFieldKey)

    if (signal == WORKFLOW_CANCEL_SIGNAL) {
        coroutineContext.cancel()
    }

    if (status != null && ActivityStatus.valueOf(status) == ActivityStatus.COMPLETED) {
        return@activity
    }

    val workflowContext = block() ?: emptyMap()

    keyValueClient.hSet(
        kotlin.coroutines.coroutineContext.getWorkflowKey(),
        activityStatusFieldKey to ActivityStatus.COMPLETED.toString(),
        *workflowContext.map { it.key.workflowContextFieldKey to it.value }.toTypedArray(),
    )
}
