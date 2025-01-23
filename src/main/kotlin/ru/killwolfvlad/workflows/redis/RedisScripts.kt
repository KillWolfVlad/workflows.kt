package ru.killwolfvlad.workflows.redis

object RedisScripts {
    /**
     * Script implementation for [ru.killwolfvlad.workflows.core.interfaces.KeyValueClient.hSetIfKeyExists]
     */
    val hSetIfKeyExistsScript =
        """
            if redis.call('EXISTS', KEYS[1]) == 1 then
                local fieldValuesSizeIndex = 1
                local fieldValuesSize = ARGV[workflowContextSizeIndex]

                local fieldValues = {}

                for i = 0, tonumber(fieldValuesSize), 1 do
                    fieldValues[i] = ARGV[fieldValuesSizeIndex + i]
                end

                redis.call('HSET', KEYS[1], unpack(fieldValues))
            end
        """.trimIndent()

    /**
     * Script implementation for [ru.killwolfvlad.workflows.core.interfaces.KeyValueClient.acquireLock]
     */
    val acquireLockScript =
        """
            local workflowKey = KEYS[1]
            local workflowIdsKey = KEYS[2]
            local workflowId = ARGV[1]
            local lockTimeout = ARGV[2]
            local workflowContextSizeIndex = 3
            local workflowContextSize = ARGV[workflowContextSizeIndex]
            local lockFieldKey = ARGV[4]
            local workerId = ARGV[5]

            if redis.call('EXISTS', workflowKey) == 0 then
                local context = {}

                for i = 0, tonumber(workflowContextSize), 1 do
                    context[i] = ARGV[workflowContextSizeIndex + i]
                end

                redis.call('HSET', workflowKey, unpack(context))
                redis.call('HPEXPIRE', workflowKey, lockTimeout, 'FIELDS', 1, lockFieldKey)
                redis.call('SADD', workflowIdsKey, workflowId)

                return 1
            end

            local currentLockWorkerId = redis.call('HGET', workflowKey, lockFieldKey)

            if currentLockWorkerId == workerId then
                redis.call('HPEXPIRE', workflowKey, lockTimeout, 'FIELDS', 1, lockFieldKey)

                return 2
            elseif type(currentLockWorkerId) == 'boolean' and not currentLockWorkerId then
                redis.call('HSET', workflowKey, lockFieldKey, workerId)
                redis.call('HPEXPIRE', workflowKey, lockTimeout, 'FIELDS', 1, lockFieldKey)

                return 3
            end

            return 0
        """.trimIndent()

    /**
     * Script implementation for [ru.killwolfvlad.workflows.core.interfaces.KeyValueClient.heartbeat]
     */
    val heartbeatScript =
        """
            local workflowKey = KEYS[1]
            local lockTimeout = ARGV[1]
            local lockFieldKey = ARGV[2]
            local signalFieldKey = ARGV[3]

            redis.call('HPEXPIRE', workflowKey, lockTimeout, 'FIELDS', 1, lockFieldKey)

            return redis.call('HGET', workflowKey, signalFieldKey)
        """.trimIndent()

    /**
     * Script implementation for [ru.killwolfvlad.workflows.core.interfaces.KeyValueClient.deleteWorkflow]
     */
    val deleteWorkflowScript =
        """
            local workflowKey = KEYS[1]
            local workflowIdsKey = KEYS[2]
            local workflowId = ARGV[1]

            redis.call('DEL', workflowKey)
            redis.call('SREM', workflowIdsKey, workflowId)
        """.trimIndent()
}
