package ru.killwolfvlad.workflows

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.annotations.ReThisInternal
import eu.vendeli.rethis.commands.get
import eu.vendeli.rethis.commands.set
import eu.vendeli.rethis.types.core.toArgument
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@OptIn(ReThisInternal::class)
class ReThisTest : DescribeSpec({
    val reThis = ReThis("localhost", 6379)

    beforeEach {
        reThis.execute(listOf("FLUSHDB").toArgument())
    }

    afterEach {
        reThis.execute(listOf("FLUSHDB").toArgument())
    }

    // ~ 10 sec
    it("set/get 100K requests") {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
            .launch {
                (1..100_000)
                    .map {
                        async {
                            reThis.set("key$it", "value$it") shouldBe "OK"
                            reThis.get("key$it") shouldBe "value$it"
                        }
                    }.awaitAll()
            }.join()
    }

    // ~1 min 24 sec
    it("set/get 1M requests") {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
            .launch {
                (1..1_000_000)
                    .map {
                        async {
                            reThis.set("key$it", "value$it") shouldBe "OK"
                            reThis.get("key$it") shouldBe "value$it"
                        }
                    }.awaitAll()
            }.join()
    }
})
