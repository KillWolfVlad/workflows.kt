package ru.killwolfvlad.workflows.core

import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.coroutines.getWorkflowKey
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import kotlin.coroutines.coroutineContext

@WorkflowsPerformance
class WorkflowContext internal constructor(
    private val keyValueClient: KeyValueClient,
) {
    suspend fun get(key: String): String? = keyValueClient.hGet(coroutineContext.getWorkflowKey(), key.workflowContextFieldKey)

    suspend fun get(vararg keys: String): Map<String, String?> {
        if (keys.isEmpty()) {
            return emptyMap()
        }

        return keyValueClient
            .hMGet(
                coroutineContext.getWorkflowKey(),
                *keys.map { it.workflowContextFieldKey }.toTypedArray(),
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
            *context.map { it.key.workflowContextFieldKey to it.value }.toTypedArray(),
        )
    }

    suspend fun delete(vararg keys: String) {
        if (keys.isEmpty()) {
            return
        }

        keyValueClient.hDel(coroutineContext.getWorkflowKey(), *keys.map { it.workflowContextFieldKey }.toTypedArray())
    }
}

internal inline val String.workflowContextFieldKey: String
    get() = "ctx:$this"
