package ru.killwolfvlad.workflows.core.internal

import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.types.WorkflowId
import kotlin.time.Duration

internal object RedisScripts {
    val heartbeat =
        """
            local workflowWorkersKey = KEYS[1]
            local workerId = ARGV[1]
            local lockTimeout = ARGV[2]

            if redis.call('HEXISTS', workflowWorkersKey, workerId) == 0 then
                redis.call('HSET', workflowWorkersKey, workerId, 'OK')
            end

            redis.call('HPEXPIRE', workflowWorkersKey, lockTimeout, 'FIELDS', 1, workerId)
        """.trimIndent()

    val acquireLock =
        """
            local workflowKey = KEYS[1]
            local workflowLocksKey = KEYS[2]
            local workflowWorkersKey = KEYS[3]
            local workflowId = ARGV[1]
            local workerId = ARGV[2]
            local workflowContextSizeIndex = 3
            local workflowContextSize = ARGV[workflowContextSizeIndex]

            if redis.call('EXISTS', workflowKey) == 0 then
                local context = {}

                for i = 1, tonumber(workflowContextSize), 1 do
                    context[i] = ARGV[workflowContextSizeIndex + i]
                end

                redis.call('HSET', workflowKey, unpack(context))
                redis.call('HSET', workflowLocksKey, workflowId, workerId)

                return 1
            end

            local currentLockWorkerId = redis.call('HGET', workflowLocksKey, workflowId)

            if currentLockWorkerId == workerId then
                return 2
            end

            if redis.call('HEXISTS', workflowWorkersKey, workerId) == 0 then
                redis.call('HSET', workflowLocksKey, workflowId, workerId)

                return 3
            end

            return 0
        """.trimIndent()

    val deleteWorkflow =
        """
            local workflowKey = KEYS[1]
            local workflowLocksKey = KEYS[2]
            local workflowId = ARGV[1]

            redis.call('DEL', workflowKey)
            redis.call('HDEL', workflowLocksKey, workflowId)
        """.trimIndent()

    val hSetIfKeyExistsScript =
        """
            if redis.call('EXISTS', KEYS[1]) == 1 then
                local fieldValuesSizeIndex = 1
                local fieldValuesSize = ARGV[fieldValuesSizeIndex]

                local fieldValues = {}

                for i = 1, tonumber(fieldValuesSize), 1 do
                    fieldValues[i] = ARGV[fieldValuesSizeIndex + i]
                end

                redis.call('HSET', KEYS[1], unpack(fieldValues))
            end
        """.trimIndent()
}

internal suspend inline fun KeyValueClient.heartbeat(
    workflowWorkersKey: String,
    workerId: String,
    lockTimeout: Duration,
) {
    eval<Unit>(
        RedisScripts::heartbeat.name,
        RedisScripts.heartbeat,
        // keys
        listOf(
            workflowWorkersKey, // KEYS[1]
        ),
        // arguments
        workerId, // ARGV[1]
        lockTimeout.inWholeMilliseconds.toString(), // ARGV[2]
    )
}

internal suspend inline fun KeyValueClient.acquireLock(
    // keys
    workflowKey: String,
    workflowLocksKey: String,
    workflowWorkersKey: String,
    // arguments
    workflowId: WorkflowId,
    workerId: String,
    // workflow context
    workflowClassNameFieldKey: String,
    workflowClassName: String,
    initialContext: Map<String, String>,
): Boolean {
    val result = eval<Long>(
        RedisScripts::acquireLock.name,
        RedisScripts.acquireLock,
        // keys
        listOf(
            workflowKey, // KEYS[1]
            workflowLocksKey, // KEYS[2]
            workflowWorkersKey, // KEYS[3]
        ),
        // arguments
        workflowId.value, // ARGV[1]
        workerId, // ARGV[2]
        ((initialContext.size + 1) * 2).toString(), // ARGV[3]
        // workflow context
        workflowClassNameFieldKey,
        workflowClassName,
        *initialContext.flatMap { listOf(it.key, it.value) }.toTypedArray()
    )

    return result >= 1L
}

internal suspend inline fun KeyValueClient.deleteWorkflow(
    // keys
    workflowKey: String,
    workflowLocksKey: String,
    // arguments
    workflowId: WorkflowId,
) {
    eval<Unit>(
        RedisScripts::deleteWorkflow.name,
        RedisScripts.deleteWorkflow,
        // keys
        listOf(
            workflowKey, // KEYS[1]
            workflowLocksKey, // KEYS[2]
        ),
        // arguments
        workflowId.value, // ARGV[1]
    )
}

internal suspend inline fun KeyValueClient.hSetIfKeyExistsScript(
    key: String,
    vararg fieldValues: Pair<String, String>,
) {
    eval<Unit>(
        RedisScripts::hSetIfKeyExistsScript.name,
        RedisScripts.hSetIfKeyExistsScript,
        // keys
        listOf(
            key, // KEYS[1]
        ),
        // arguments
        (fieldValues.size * 2).toString(), // ARGV[1]
        *fieldValues.flatMap { listOf(it.first, it.second) }.toTypedArray()
    )
}
