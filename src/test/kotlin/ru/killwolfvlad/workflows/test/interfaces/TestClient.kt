package ru.killwolfvlad.workflows.test.interfaces

import io.kotest.mpp.bestName
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient

interface TestClient {
    val dbName: String

    val keyValueClient: KeyValueClient

    val testKeyValueClient: TestKeyValueClient

    val name: String
        get() = "${keyValueClient::class.bestName()} [$dbName]"
}
