package ru.killwolfvlad.workflows.redis

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.commands.*
import eu.vendeli.rethis.types.core.Int64
import eu.vendeli.rethis.types.core.RType
import io.ktor.util.collections.*
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.workflowContextFieldKey
import kotlin.time.Duration

class ReThisClient(
    private val client: ReThis,
) : KeyValueClient {
    //region HASH

    override suspend fun hGet(key: String, field: String): String? =
        client.hGet(key, field)

    override suspend fun hMGet(key: String, vararg fields: String): List<String?> =
        // TODO: send PR to ReThis with fix return type, must be List<String?>, not List<String>
        client.hMGet(key, *fields)

    override suspend fun hSet(key: String, vararg fieldValues: Pair<String, String>) {
        // TODO: send PR to ReThis with fix return type, must be not null
        client.hSet(key, *fieldValues)
    }

    override suspend fun hDel(key: String, vararg fields: String) {
        client.hDel(key, *fields)
    }

    //endregion

    //region SET

    override suspend fun sMembers(key: String): Set<String> =
        client.sMembers(key)

    //endregion

    //region PUB/SUB

    override suspend fun publish(channel: String, message: String) {
        client.publish(channel, message)
    }

    override suspend fun subscribe(channel: String, handler: suspend (message: String) -> Unit) =
        client.subscribe(channel) { messageClient, message ->
            handler(message)
        }

    //endregion

    //region PIPELINES

    override suspend fun pipelineHGet(vararg keyFields: Pair<String, String>): List<String?> =
        client.pipeline {
            keyFields.forEach {
                hGet(it.first, it.second)
            }
        }.map {
            it.safeGetValue() as String?
        }

    //endregion

    // region SCRIPTS

    override suspend fun hSetIfKeyExists(
        key: String,
        vararg fieldValues: Pair<String, String>,
    ) {
        client.fastEval(
            "hSetIfKeyExists",
            RedisScripts.hSetIfKeyExistsScript,
            1,
            key, // KEYS[1]
            (fieldValues.size * 2).toString(), // ARGV[1]
            *fieldValues.flatMap { listOf(it.first, it.second) }.toTypedArray()
        )
    }

    override suspend fun acquireLock(
        // keys
        workflowKey: String,
        workflowIdsKey: String,
        // arguments
        workflowId: WorkflowId,
        lockTimeout: Duration,
        // workflow context
        workflowLockFieldKey: String,
        workerId: String,
        workflowClassNameFieldKey: String,
        workflowClassName: String,
        initialContext: Map<String, String>,
    ): Boolean {
        val result = client.fastEval(
            "acquireLock",
            RedisScripts.acquireLockScript,
            // keys
            2,
            workflowKey, // KEYS[1]
            workflowIdsKey, // KEYS[2]
            // arguments
            workflowId.value, // ARGV[1]
            lockTimeout.inWholeMilliseconds.toString(), // ARGV[2]
            ((initialContext.size + 2) * 2).toString(), // ARGV[3]
            // workflow context
            workflowLockFieldKey, // ARGV[4]
            workerId, // ARGV[5]
            workflowClassNameFieldKey,
            workflowClassName,
            *initialContext.flatMap { listOf(it.key.workflowContextFieldKey, it.value) }.toTypedArray()
        )

        return (result as Int64).value >= 1L
    }

    override suspend fun heartbeat(
        workflowKey: String,
        lockTimeout: Duration,
        workflowLockFieldKey: String,
        workflowSignalFieldKey: String,
    ): String? {
        val result = client.fastEval(
            "heartbeat",
            RedisScripts.heartbeatScript,
            // keys
            1,
            workflowKey,
            // arguments
            lockTimeout.inWholeMilliseconds.toString(), // ARGV[1]
            workflowLockFieldKey, // ARGV[2]
            workflowSignalFieldKey, // ARGV[3]
        )

        return result.value as String?
    }

    override suspend fun deleteWorkflow(
        workflowKey: String,
        workflowIdsKey: String,
        workflowId: WorkflowId,
    ) {
        client.fastEval(
            "deleteWorkflow",
            RedisScripts.deleteWorkflowScript,
            // keys
            2,
            workflowKey,
            workflowIdsKey,
            // arguments
            workflowId.value,
        )
    }

    // endregion
}

private val scriptsSha1Map = ConcurrentMap<String, String>()

private suspend inline fun ReThis.fastEval(
    scriptId: String,
    script: String,
    numKeys: Long,
    vararg keys: String,
): RType {
    var sha1 = scriptsSha1Map[scriptId]

    if (sha1 == null) {
        sha1 = scriptLoad(script) ?: throw NullPointerException("script load don't return sha1 hash!")

        scriptsSha1Map[scriptId] = sha1
    }

    var result = evalSha(sha1, numKeys, *keys)

    if (result is RType.Error) {
        if (result.exception.message == "NOSCRIPT No matching script. Please use EVAL.") {
            scriptLoad(script)

            result = evalSha(sha1, numKeys, *keys)

            if (result is RType.Error) {
                throw result.exception
            }
        } else {
            throw result.exception
        }
    }

    return result
}

private inline fun RType.safeGetValue(): Any? =
    when (this) {
        is RType.Error -> throw exception
        else -> value
    }
