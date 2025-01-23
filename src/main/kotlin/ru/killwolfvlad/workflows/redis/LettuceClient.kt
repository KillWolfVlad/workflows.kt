@file:OptIn(ExperimentalLettuceCoroutinesApi::class)

package ru.killwolfvlad.workflows.redis

import io.ktor.util.collections.*
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.workflowContextFieldKey
import kotlin.time.Duration

class LettuceClient : KeyValueClient {
    private val redis = RedisClient.create("redis://localhost:6379")
    private val client = redis.connect().coroutines()
    private val conPubSub = redis.connectPubSub()
    private val clientPubSub = conPubSub.sync()

    //region HASH

    override suspend fun hGet(key: String, field: String): String? =
        client.hget(key, field)

    override suspend fun hMGet(key: String, vararg fields: String): List<String?> =
        client.hmget(key, *fields).map { it.value }.toList()

    override suspend fun hSet(key: String, vararg fieldValues: Pair<String, String>) {
        client.hset(key, fieldValues.toMap())
    }

    override suspend fun hDel(key: String, vararg fields: String) {
        client.hdel(key, *fields)
    }

    //endregion

    //region SET

    override suspend fun sMembers(key: String): Set<String> =
        client.smembers(key).toSet()

    //endregion

    //region PUB/SUB

    override suspend fun publish(channel: String, message: String) {
        client.publish(channel, message)
    }

    override suspend fun subscribe(channel: String, handler: suspend (message: String) -> Unit) {
        conPubSub.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                // TODO: remove blocking call
                runBlocking {
                    handler(message)
                }
            }
        })

        clientPubSub.subscribe(channel)
    }

    //endregion

    //region PIPELINES

    override suspend fun pipelineHGet(vararg keyFields: Pair<String, String>): List<String?> {
        // TODO: use pipeline here!

        val result = mutableListOf<String?>()

        keyFields.forEach {
            result.add(hGet(it.first, it.second))
        }

        return result
    }

    //endregion

    // region SCRIPTS

    override suspend fun hSetIfKeyExists(
        key: String,
        vararg fieldValues: Pair<String, String>,
    ) {
        client.fastEval<String, String, String>(
            "hSetIfKeyExists",
            RedisScripts.hSetIfKeyExistsScript,
            ScriptOutputType.STATUS,
            listOf(key),
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
        val result = client.fastEval<String, String, Long>(
            "acquireLock",
            RedisScripts.acquireLockScript,
            ScriptOutputType.INTEGER,
            listOf(
                workflowKey,
                workflowIdsKey,
            ),
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

        return result != null && result >= 1L
    }

    override suspend fun heartbeat(
        workflowKey: String,
        lockTimeout: Duration,
        workflowLockFieldKey: String,
        workflowSignalFieldKey: String,
    ): String? {
        val result = client.fastEval<String, String, String>(
            "heartbeat",
            RedisScripts.heartbeatScript,
            ScriptOutputType.VALUE,
            listOf(workflowKey),
            // arguments
            lockTimeout.inWholeMilliseconds.toString(), // ARGV[1]
            workflowLockFieldKey, // ARGV[2]
            workflowSignalFieldKey, // ARGV[3]
        )

        return result
    }

    override suspend fun deleteWorkflow(
        workflowKey: String,
        workflowIdsKey: String,
        workflowId: WorkflowId,
    ) {
        client.fastEval<String, String, String>(
            "deleteWorkflow",
            RedisScripts.deleteWorkflowScript,
            ScriptOutputType.STATUS,
            listOf(
                workflowKey,
                workflowIdsKey,
            ),
            // arguments
            workflowId.value,
        )
    }

    // endregion
}

private val scriptsSha1Map = ConcurrentMap<String, String>()

private suspend inline fun <reified K : Any, reified V : Any, reified T> RedisCoroutinesCommands<K, V>.fastEval(
    scriptId: String,
    script: String,
    type: ScriptOutputType,
    keys: List<K>,
    vararg values: V,
): T? {
    var sha1 = scriptsSha1Map[scriptId]

    if (sha1 == null) {
        sha1 = scriptLoad(script) ?: throw NullPointerException("script load don't return sha1 hash!")

        scriptsSha1Map[scriptId] = sha1
    }

    var result = evalsha<T>(sha1, type, keys.toTypedArray(), *values)

    // TODO: add check to exception
//    if (result is RType.Error) {
//        if (result.exception.message == "NOSCRIPT No matching script. Please use EVAL.") {
//            scriptLoad(script)
//
//            result = evalSha(sha1, numKeys, *keys)
//
//            if (result is RType.Error) {
//                throw result.exception
//            }
//        } else {
//            throw result.exception
//        }
//    }

    return result
}
