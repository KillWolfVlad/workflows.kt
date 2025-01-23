package ru.killwolfvlad.workflows.interfaces

import ru.killwolfvlad.workflows.types.WorkflowId
import kotlin.time.Duration

/**
 * An abstraction over key-value storage, e.g. Redis
 */
interface KeyValueClient {
    //region HASH

    /**
     * @see <a href="https://redis.io/docs/latest/commands/hget">Redis HGET</a>
     */
    suspend fun hGet(key: String, field: String): String?

    /**
     * @see <a href="https://redis.io/docs/latest/commands/hmget">Redis HMGET</a>
     */
    suspend fun hMGet(key: String, vararg fields: String): List<String?>

    /**
     * @see <a href="https://redis.io/docs/latest/commands/hset">Redis HSET</a>
     */
    suspend fun hSet(key: String, vararg fieldValues: Pair<String, String>)

    /**
     * @see <a href="https://redis.io/docs/latest/commands/hdel">Redis HDEL</a>
     */
    suspend fun hDel(key: String, vararg fields: String)

    //endregion

    //region SET

    /**
     * @see <a href="https://redis.io/docs/latest/commands/smembers">Redis SMEMBERS</a>
     */
    suspend fun sMembers(key: String): Set<String>

    //endregion

    //region PUB/SUB

    /**
     * @see <a href="https://redis.io/docs/latest/commands/publish">Redis PUBLISH</a>
     */
    suspend fun publish(channel: String, message: String)

    /**
     * @see <a href="https://redis.io/docs/latest/commands/subscribe">Redis SUBSCRIBE</a>
     */
    suspend fun subscribe(channel: String, handler: suspend (message: String) -> Unit)

    //endregion

    //region PIPELINES

    /**
     * Invoke multiple [hGet] requests at once
     */
    suspend fun pipelineHGet(vararg keyFields: Pair<String, String>): List<String?>

    //endregion

    // region SCRIPTS

    /**
     * Invoke [hSet], only if [key] exists,
     *
     * See [ru.killwolfvlad.workflows.redis.RedisScripts.hSetIfKeyExistsScript] for implementation
     */
    suspend fun hSetIfKeyExists(key: String, vararg fieldValues: Pair<String, String>)

    /**
     * TODO: add docs
     *
     * See [ru.killwolfvlad.workflows.redis.RedisScripts.acquireLockScript] for implementation
     */
    suspend fun acquireLock(
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
    ): Boolean

    /**
     * TODO: add docs
     *
     * See [ru.killwolfvlad.workflows.redis.RedisScripts.heartbeatScript] for implementation
     */
    suspend fun heartbeat(
        // keys
        workflowKey: String,
        // arguments
        lockTimeout: Duration,
        workflowLockFieldKey: String,
        workflowSignalFieldKey: String,
    ): String?

    /**
     * TODO: add docs
     *
     * See [ru.killwolfvlad.workflows.redis.RedisScripts.deleteWorkflowScript] for implementation
     */
    suspend fun deleteWorkflow(
        // keys
        workflowKey: String,
        workflowIdsKey: String,
        // arguments
        workflowId: WorkflowId,
    )

    // endregion
}
