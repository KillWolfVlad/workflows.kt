package ru.killwolfvlad.workflows.clients

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import java.util.concurrent.ConcurrentHashMap

@WorkflowsPerformance
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceRedisClient(
    rootJob: Job,
    redisClient: RedisClient,
) : KeyValueClient {
    private val connection = redisClient.connect()
    private val connectionPubSub = redisClient.connectPubSub()
    private val commands = connection.coroutines()
    private val commandsPubSub = connectionPubSub.reactive()

    private val coroutineScopePubSub =
        CoroutineScope(
            rootJob + Dispatchers.IO + CoroutineName(LettuceRedisClient::class.simpleName + "CoroutinePubSub"),
        )

    //region HASH

    override suspend fun hGet(
        key: String,
        field: String,
    ): String? = commands.hget(key, field)

    override suspend fun hMGet(
        key: String,
        vararg fields: String,
    ): List<String?> = commands.hmget(key, *fields).map { if (it.hasValue()) it.value else null }.toList()

    override suspend fun hSet(
        key: String,
        vararg fieldValues: Pair<String, String>,
    ) {
        commands.hset(key, fieldValues.toMap())
    }

    override suspend fun hDel(
        key: String,
        vararg fields: String,
    ) {
        commands.hdel(key, *fields)
    }

    //endregion

    //region PUB/SUB

    override suspend fun publish(
        channel: String,
        message: String,
    ) {
        commands.publish(channel, message)
    }

    override suspend fun subscribe(
        channel: String,
        handler: suspend (message: String) -> Unit,
    ) {
        coroutineScopePubSub.launch {
            commandsPubSub.observeChannels().asFlow().collect { channelMessage ->
                if (channelMessage.channel == channel) {
                    handler(channelMessage.message)
                }
            }
        }

        commandsPubSub.subscribe(channel).awaitFirstOrNull()
    }

    //endregion

    //region PIPELINES

    override suspend fun pipelineHGet(vararg keyFields: Pair<String, String>): List<String?> =
        withContext(Dispatchers.IO) {
            keyFields
                .map {
                    async {
                        commands.hget(it.first, it.second)
                    }
                }.awaitAll()
        }

    override suspend fun pipelineHGetAll(vararg keys: String): List<Map<String, String>> =
        withContext(Dispatchers.IO) {
            keys
                .map {
                    async {
                        commands.hgetall(it).toList().associate { it.key to it.value }
                    }
                }.awaitAll()
        }

    //endregion

    // region SCRIPTS

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> eval(
        scriptId: String,
        script: String,
        keys: List<String>,
        vararg args: String,
    ): T = commands.fastEval<String, String, Any>(scriptId, script, keys, *args) as T

    // endregion
}

private val scriptsSha1Map = ConcurrentHashMap<String, String>()

@ExperimentalLettuceCoroutinesApi
private suspend inline fun <reified K : Any, reified V : Any, reified T> RedisCoroutinesCommands<K, V>.fastEval(
    scriptId: String,
    script: String,
    keys: List<K>,
    vararg values: V,
): T? {
    var sha1 = scriptsSha1Map[scriptId]

    if (sha1 == null) {
        sha1 = scriptLoad(script) ?: throw NullPointerException("script load don't return sha1 hash!")

        scriptsSha1Map[scriptId] = sha1
    }

    var result =
        try {
            evalsha<T>(sha1, ScriptOutputType.INTEGER, keys.toTypedArray(), *values)
        } catch (exception: Exception) {
            if (exception.message == "NOSCRIPT No matching script. Please use EVAL.") {
                scriptLoad(script)

                evalsha<T>(sha1, ScriptOutputType.INTEGER, keys.toTypedArray(), *values)
            } else {
                throw exception
            }
        }

    return result
}
