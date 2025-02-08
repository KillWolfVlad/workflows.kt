package ru.killwolfvlad.workflows.core

import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.coroutines.getActivityId
import ru.killwolfvlad.workflows.core.coroutines.getWorkflowKey
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.internal.consts.ACTIVITY_FIELD_KEY_PREFIX
import kotlin.coroutines.coroutineContext

@WorkflowsPerformance
class ActivityContext internal constructor(
    private val keyValueClient: KeyValueClient,
) {
    suspend fun get(key: String): String? = keyValueClient.hGet(coroutineContext.getWorkflowKey(), key.getActivityContextFieldKey())

    suspend fun get(vararg keys: String): Map<String, String?> {
        if (keys.isEmpty()) {
            return emptyMap()
        }

        return keyValueClient
            .hMGet(
                coroutineContext.getWorkflowKey(),
                *keys.map { it.getActivityContextFieldKey() }.toTypedArray(),
            ).zip(keys) { a, b ->
                b to a
            }.toMap()
    }

    suspend fun set(context: Map<String, String>) {
        if (context.isEmpty()) {
            return
        }

        keyValueClient.hSet(
            coroutineContext.getWorkflowKey(),
            *context.map { it.key.getActivityContextFieldKey() to it.value }.toTypedArray(),
        )
    }

    suspend fun delete(vararg keys: String) {
        if (keys.isEmpty()) {
            return
        }

        keyValueClient.hDel(
            coroutineContext.getWorkflowKey(),
            *keys.map { it.getActivityContextFieldKey() }.toTypedArray(),
        )
    }
}

internal suspend inline fun String.getActivityContextFieldKey(): String =
    "${ACTIVITY_FIELD_KEY_PREFIX}:${coroutineContext.getActivityId()}:ctx:$this"
