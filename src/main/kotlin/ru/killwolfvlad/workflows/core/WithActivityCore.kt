package ru.killwolfvlad.workflows.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.coroutines.ActivityCoroutineContext
import ru.killwolfvlad.workflows.core.coroutines.getKeyValueClient
import ru.killwolfvlad.workflows.core.coroutines.getWorkflowKey
import ru.killwolfvlad.workflows.core.internal.enums.ActivityStatus
import ru.killwolfvlad.workflows.core.internal.enums.WorkflowSignal
import ru.killwolfvlad.workflows.core.types.ActivityCallback
import kotlin.coroutines.coroutineContext

@OptIn(WorkflowsPerformance::class)
suspend fun withActivity(
    activityId: String,
    workflowContextKeys: List<String> = emptyList(),
    activityContextKeys: List<String> = emptyList(),
    block: ActivityCallback,
) {
    val actualActivityId =
        coroutineContext[ActivityCoroutineContext]?.let {
            "${it.activityId}:$activityId"
        } ?: activityId

    withContext(ActivityCoroutineContext(actualActivityId)) activity@{
        val keyValueClient = coroutineContext.getKeyValueClient()

        val values =
            keyValueClient.hMGet(
                coroutineContext.getWorkflowKey(),
                WorkflowSignal.FIELD_KEY,
                ActivityStatus.getFieldKey(),
                *workflowContextKeys.map { it.workflowContextFieldKey }.toTypedArray(),
                *activityContextKeys.map { it.getActivityContextFieldKey() }.toTypedArray(),
            )

        val signal = values[0]?.let { WorkflowSignal.valueOf(it) }

        if (signal == WorkflowSignal.CANCEL) {
            throw CancellationException()
        }

        val status = values[1]?.let { ActivityStatus.valueOf(it) }

        if (status == ActivityStatus.COMPLETED) {
            return@activity
        }

        val workflowContextMap =
            values
                .slice(2..(workflowContextKeys.size + 1))
                .zip(workflowContextKeys) { a, b ->
                    b to a
                }.toMap()

        val activityContextMap =
            values
                .slice((workflowContextKeys.size + 2) until values.size)
                .zip(activityContextKeys) { a, b ->
                    b to a
                }.toMap()

        val returnedWorkflowContextMap = block(workflowContextMap, activityContextMap) ?: emptyMap()

        keyValueClient.hSet(
            coroutineContext.getWorkflowKey(),
            ActivityStatus.getFieldKey() to ActivityStatus.COMPLETED.toString(),
            *returnedWorkflowContextMap.map { it.key.workflowContextFieldKey to it.value }.toTypedArray(),
        )
    }
}
