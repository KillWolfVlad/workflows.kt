package ru.killwolfvlad.workflows

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class LettuceTest : DescribeSpec({
    val redisClient = RedisClient.create("redis://localhost:6379")
    val connection = redisClient.connect()
    val commands = connection.coroutines()

    beforeEach {
        commands.flushdb()
    }

    afterEach {
        commands.flushdb()
    }

    // ~11 sec
    it("set/get 100K requests") {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
            .launch {
                (1..100_000)
                    .map {
                        async {
                            commands.set("key$it", "value$it") shouldBe "OK"
                            commands.get("key$it") shouldBe "value$it"
                        }
                    }.awaitAll()
            }.join()
    }

    // ~1 min 26 sec
    it("set/get 1M requests") {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
            .launch {
                (1..1_000_000)
                    .map {
                        async {
                            commands.set("key$it", "value$it") shouldBe "OK"
                            commands.get("key$it") shouldBe "value$it"
                        }
                    }.awaitAll()
            }.join()
    }
})
