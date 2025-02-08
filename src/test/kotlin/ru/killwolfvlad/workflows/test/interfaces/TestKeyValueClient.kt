package ru.killwolfvlad.workflows.test.interfaces

interface TestKeyValueClient {
    suspend fun flushDB()

    suspend fun scriptFlush()

    suspend fun hGet(
        key: String,
        field: String,
    ): String?

    suspend fun hSet(
        key: String,
        vararg fieldValues: Pair<String, String>,
    )

    suspend fun hPTTL(
        key: String,
        vararg fields: String,
    ): List<Long>

    suspend fun hGetAll(key: String): Map<String, String>

    suspend fun hExists(
        key: String,
        field: String,
    ): Boolean

    suspend fun exists(vararg keys: String): Long

    suspend fun keys(pattern: String): List<String>
}
