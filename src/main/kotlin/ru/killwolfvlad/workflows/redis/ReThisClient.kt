package ru.killwolfvlad.workflows.redis

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.commands.*
import eu.vendeli.rethis.types.core.RType
import io.ktor.util.collections.*
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient

class ReThisClient(
    private val client: ReThis,
) : KeyValueClient {
    //region HASH

    override suspend fun hGet(key: String, field: String): String? =
        client.hGet(key, field)

    override suspend fun hMGet(key: String, vararg fields: String): List<String?> =
        client.hMGet(key, *fields)

    override suspend fun hSet(key: String, vararg fieldValues: Pair<String, String>) {
        client.hSet(key, *fieldValues)
    }

    override suspend fun hDel(key: String, vararg fields: String) {
        client.hDel(key, *fields)
    }

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

    // TODO: fix bug with pipeline
    override suspend fun pipelineHGetAll(vararg keys: String): List<Map<String, String>> =
        client.pipeline {
            keys.forEach {
                hGetAll(it)
            }
        }.map {
            it.safeGetValue() as Map<String, String>
        }

    //endregion

    // region SCRIPTS

    override suspend fun <T> eval(
        scriptId: String,
        script: String,
        keys: List<String>,
        vararg args: String,
    ): T =
        client.fastEval(scriptId, script, keys.size.toLong(), *keys.toTypedArray(), *args).value as T

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
