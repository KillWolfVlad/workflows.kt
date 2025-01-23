package ru.killwolfvlad.workflows.activities

import ru.killwolfvlad.workflows.activities.coroutines.getActivityId
import ru.killwolfvlad.workflows.activities.internal.consts.ACTIVITY_CONTEXT_FIELD_KEY_PREFIX
import ru.killwolfvlad.workflows.core.coroutines.getWorkflowKey
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.workflowContextFieldKey
import kotlin.coroutines.coroutineContext

class ActivityContext(
    private val keyValueClient: KeyValueClient,
) {
    suspend fun get(key: String): String? =
        keyValueClient.hGet(coroutineContext.getWorkflowKey(), key.getActivityContextFieldKey())

    suspend fun get(vararg keys: String): Map<String, String?> {
        if (keys.isEmpty()) {
            return emptyMap()
        }

        return keyValueClient.hMGet(
            coroutineContext.getWorkflowKey(),
            *keys.map { it.getActivityContextFieldKey() }.toTypedArray(),
        ).zip(keys) { a, b ->
            b to a
        }.toMap()
    }

    suspend fun set(context: Map<String, String>, workflowContext: Map<String, String> = emptyMap()) {
        if (context.isEmpty() && workflowContext.isEmpty()) {
            return
        }

        keyValueClient.hSet(
            coroutineContext.getWorkflowKey(),
            *context.map { it.key.getActivityContextFieldKey() to it.value }.toTypedArray(),
            *workflowContext.map { it.key.workflowContextFieldKey to it.value }.toTypedArray(),
        )
    }

    suspend fun delete(vararg keys: String) {
        if (keys.isEmpty()) {
            return
        }

        keyValueClient.hDel(
            coroutineContext.getWorkflowKey(),
            *keys.map { it.getActivityContextFieldKey() }.toTypedArray()
        )
    }
}

internal suspend inline fun String.getActivityContextFieldKey(): String =
    "${ACTIVITY_CONTEXT_FIELD_KEY_PREFIX}:${coroutineContext.getActivityId()}:$this"
