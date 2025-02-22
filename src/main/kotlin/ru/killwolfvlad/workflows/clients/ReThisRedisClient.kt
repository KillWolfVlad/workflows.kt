package ru.killwolfvlad.workflows.clients

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.commands.evalSha
import eu.vendeli.rethis.commands.hDel
import eu.vendeli.rethis.commands.hGet
import eu.vendeli.rethis.commands.hGetAll
import eu.vendeli.rethis.commands.hMGet
import eu.vendeli.rethis.commands.hSet
import eu.vendeli.rethis.commands.publish
import eu.vendeli.rethis.commands.scriptLoad
import eu.vendeli.rethis.commands.subscribe
import eu.vendeli.rethis.types.common.RType
import eu.vendeli.rethis.utils.unwrap
import eu.vendeli.rethis.utils.unwrapMap
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import java.util.concurrent.ConcurrentHashMap

@WorkflowsPerformance
class ReThisRedisClient(
    private val client: ReThis,
) : KeyValueClient {
    //region HASH

    override suspend fun hGet(
        key: String,
        field: String,
    ): String? = client.hGet(key, field)

    override suspend fun hMGet(
        key: String,
        vararg fields: String,
    ): List<String?> = client.hMGet(key, *fields)

    override suspend fun hSet(
        key: String,
        vararg fieldValues: Pair<String, String>,
    ) {
        client.hSet(key, *fieldValues)
    }

    override suspend fun hDel(
        key: String,
        vararg fields: String,
    ) {
        client.hDel(key, *fields)
    }

    //endregion

    //region PUB/SUB

    override suspend fun publish(
        channel: String,
        message: String,
    ) {
        client.publish(channel, message)
    }

    override suspend fun subscribe(
        channel: String,
        handler: suspend (message: String) -> Unit,
    ) = client.subscribe(channel) { messageClient, message ->
        handler(message)
    }

    //endregion

    //region PIPELINES

    override suspend fun pipelineHGet(vararg keyFields: Pair<String, String>): List<String?> =
        client
            .pipeline {
                keyFields.forEach {
                    hGet(it.first, it.second)
                }
            }.map {
                it.unwrap<String>()
            }

    @Suppress("UNCHECKED_CAST")
    override suspend fun pipelineHGetAll(vararg keys: String): List<Map<String, String>> =
        client
            .pipeline {
                keys.forEach {
                    hGetAll(it)
                }
            }.map {
                it.unwrapMap<String, String>() as Map<String, String>
            }

    //endregion

    // region SCRIPTS

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> eval(
        scriptId: String,
        script: String,
        keys: List<String>,
        vararg args: String,
    ): T = client.fastEval(scriptId, script, keys.size.toLong(), *keys.toTypedArray(), *args).value as T

    // endregion
}

private val scriptsSha1Map = ConcurrentHashMap<String, String>()

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
