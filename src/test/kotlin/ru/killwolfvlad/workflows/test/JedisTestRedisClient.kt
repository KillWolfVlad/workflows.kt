package ru.killwolfvlad.workflows.test

import redis.clients.jedis.JedisPooled
import ru.killwolfvlad.workflows.test.interfaces.TestKeyValueClient

class JedisTestRedisClient(
    private val client: JedisPooled,
) : TestKeyValueClient {
    override suspend fun flushDB() {
        client.flushDB()
    }

    override suspend fun scriptFlush() {
        client.scriptFlush()
    }

    override suspend fun hGet(
        key: String,
        field: String,
    ): String? = client.hget(key, field)

    override suspend fun hSet(
        key: String,
        vararg fieldValues: Pair<String, String>,
    ) {
        client.hset(key, fieldValues.toMap())
    }

    override suspend fun hPTTL(
        key: String,
        vararg fields: String,
    ): List<Long> = client.hpttl(key, *fields)

    override suspend fun hGetAll(key: String): Map<String, String> = client.hgetAll(key)

    override suspend fun hExists(
        key: String,
        field: String,
    ): Boolean = client.hexists(key, field)

    override suspend fun exists(vararg keys: String): Long = client.exists(*keys)

    override suspend fun keys(pattern: String): List<String> = client.keys(pattern).toList()
}
