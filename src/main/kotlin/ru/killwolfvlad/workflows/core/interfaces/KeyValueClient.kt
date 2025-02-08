package ru.killwolfvlad.workflows.core.interfaces

import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance

/**
 * An abstraction over key-value storage, e.g. Redis
 */
@WorkflowsPerformance
interface KeyValueClient {
    //region HASH

    /**
     * @see <a href="https://redis.io/docs/latest/commands/hget">Redis HGET</a>
     */
    suspend fun hGet(
        key: String,
        field: String,
    ): String?

    /**
     * @see <a href="https://redis.io/docs/latest/commands/hmget">Redis HMGET</a>
     */
    suspend fun hMGet(
        key: String,
        vararg fields: String,
    ): List<String?>

    /**
     * @see <a href="https://redis.io/docs/latest/commands/hset">Redis HSET</a>
     */
    suspend fun hSet(
        key: String,
        vararg fieldValues: Pair<String, String>,
    )

    /**
     * @see <a href="https://redis.io/docs/latest/commands/hdel">Redis HDEL</a>
     */
    suspend fun hDel(
        key: String,
        vararg fields: String,
    )

    //endregion

    //region PUB/SUB

    /**
     * @see <a href="https://redis.io/docs/latest/commands/publish">Redis PUBLISH</a>
     */
    suspend fun publish(
        channel: String,
        message: String,
    )

    /**
     * @see <a href="https://redis.io/docs/latest/commands/subscribe">Redis SUBSCRIBE</a>
     */
    suspend fun subscribe(
        channel: String,
        handler: suspend (message: String) -> Unit,
    )

    //endregion

    //region PIPELINES

    /**
     * Invoke multiple `Redis HGET` requests at once
     * @see <a href="https://redis.io/docs/latest/commands/hget">Redis HGET</a>
     */
    suspend fun pipelineHGet(vararg keyFields: Pair<String, String>): List<String?>

    /**
     * Invoke multiple `Redis HGETALL` requests at once
     * @see <a href="https://redis.io/docs/latest/commands/hgetall">Redis HGETALL</a>
     */
    suspend fun pipelineHGetAll(vararg keys: String): List<Map<String, String>>

    //endregion

    // region SCRIPTS

    /**
     * Eval [script]. Use [scriptId] to associate script with their hash.
     * @see <a href="https://redis.io/docs/latest/commands/evalsha">Redis EVALSHA</a>
     */
    suspend fun <T> eval(
        scriptId: String,
        script: String,
        keys: List<String>,
        vararg args: String,
    ): T

    // endregion
}
