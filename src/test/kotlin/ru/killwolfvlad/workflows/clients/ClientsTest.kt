package ru.killwolfvlad.workflows.clients

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec
import kotlin.time.Duration.Companion.seconds

class ClientsTest : WorkflowsDescribeSpec({
    for (testClient in testClients) {
        val keyValueClient = testClient.keyValueClient
        val testKeyValueClient = testClient.testKeyValueClient

        describe(testClient.name) {
            beforeEach {
                testKeyValueClient.flushDB()
            }

            describe("hGet") {
                describe("when key doesn't exists") {
                    it("must return null") {
                        keyValueClient.hGet("key", "field1") shouldBe null
                    }
                }

                describe("when field doesn't exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key", "field2" to "value2")
                    }

                    it("must return null") {
                        keyValueClient.hGet("key", "field1") shouldBe null
                    }
                }

                describe("when field exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key", "field1" to "value1", "field2" to "value2")
                    }

                    it("must return value") {
                        keyValueClient.hGet("key", "field1") shouldBe "value1"
                    }
                }
            }

            describe("hMGet") {
                describe("when keys doesn't exists") {
                    it("must return nulls") {
                        keyValueClient.hMGet("key", "field1", "field2") shouldBe listOf(null, null)
                    }
                }

                describe("when fields doesn't exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key", "field3" to "value3")
                    }

                    it("must return nulls") {
                        keyValueClient.hMGet("key", "field1", "field2") shouldBe listOf(null, null)
                    }
                }

                describe("when some fields exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key", "field2" to "value2")
                    }

                    it("must return values and nulls") {
                        keyValueClient.hMGet("key", "field1", "field2") shouldBe listOf(null, "value2")
                    }
                }

                describe("when fields exists") {
                    beforeEach {
                        testKeyValueClient.hSet(
                            "key",
                            "field1" to "value1",
                            "field2" to "value2",
                            "field3" to "value3",
                        )
                    }

                    it("must return values") {
                        keyValueClient.hMGet("key", "field1", "field2") shouldBe listOf("value1", "value2")
                    }
                }
            }

            describe("hSet") {
                describe("when key doesn't exists") {
                    beforeEach {
                        keyValueClient.hSet("key", "field1" to "value1", "field2" to "value2")
                    }

                    it("must set values") {
                        testKeyValueClient.hGetAll("key") shouldBe
                            mapOf(
                                "field1" to "value1",
                                "field2" to "value2",
                            )
                    }
                }

                describe("when key exists") {
                    beforeEach {
                        keyValueClient.hSet("key", "field1" to "value1", "field2" to "value2")
                    }

                    describe("when fields doesn't exists") {
                        beforeEach {
                            keyValueClient.hSet("key", "field3" to "value3")
                        }

                        it("must set values") {
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
                                    "field1" to "value1",
                                    "field2" to "value2",
                                    "field3" to "value3",
                                )
                        }
                    }

                    describe("when some fields exists") {
                        beforeEach {
                            keyValueClient.hSet("key", "field1" to "value11", "field3" to "value3")
                        }

                        it("must overwrite values") {
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
                                    "field1" to "value11",
                                    "field2" to "value2",
                                    "field3" to "value3",
                                )
                        }
                    }

                    describe("when fields exists") {
                        beforeEach {
                            keyValueClient.hSet("key", "field1" to "value11", "field2" to "value22")
                        }

                        it("must overwrite values") {
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
                                    "field1" to "value11",
                                    "field2" to "value22",
                                )
                        }
                    }
                }
            }

            describe("hDel") {
                describe("when key doesn't exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key2", "field1" to "value1")

                        keyValueClient.hDel("key1", "field1", "field2")
                    }

                    it("must do nothing") {
                        testKeyValueClient.hGetAll("key2") shouldBe
                            mapOf(
                                "field1" to "value1",
                            )
                    }
                }

                describe("when key exists") {
                    beforeEach {
                        testKeyValueClient.hSet(
                            "key",
                            "field1" to "value1",
                            "field2" to "value2",
                            "field3" to "value3",
                        )
                    }

                    describe("when fields doesn't exist") {
                        beforeEach {
                            keyValueClient.hDel("key", "field4", "field5")
                        }

                        it("must do nothing") {
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
                                    "field1" to "value1",
                                    "field2" to "value2",
                                    "field3" to "value3",
                                )
                        }
                    }

                    describe("when some fields exists") {
                        beforeEach {
                            keyValueClient.hDel("key", "field1", "field4")
                        }

                        it("must delete only existing fields") {
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
                                    "field2" to "value2",
                                    "field3" to "value3",
                                )
                        }
                    }

                    describe("when fields exists") {
                        beforeEach {
                            keyValueClient.hDel("key", "field1", "field2")
                        }

                        it("must delete fields") {
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
                                    "field3" to "value3",
                                )
                        }
                    }
                }
            }

            describe("publish/subscribe") {
                var receivedMessage: String? = null

                beforeEach {
                    keyValueClient.subscribe(testClient.name) { message ->
                        receivedMessage = message
                    }

                    keyValueClient.publish(testClient.name, "some-message")
                }

                it("must receive message") {
                    eventually(2.seconds) {
                        receivedMessage shouldBe "some-message"
                    }
                }
            }

            describe("pipelineHGet") {
                beforeEach {
                    testKeyValueClient.hSet("key3", "field31" to "value31")
                }

                describe("when keys doesn't exists") {
                    it("must return nulls") {
                        keyValueClient.pipelineHGet(
                            "key1" to "field11",
                            "key2" to "field22",
                        ) shouldBe listOf(null, null)
                    }
                }

                describe("when some keys exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key1", "field11" to "value11", "field12" to "value12")
                        testKeyValueClient.hSet("key3", "field31" to "value31")
                    }

                    it("must return values and nulls") {
                        keyValueClient.pipelineHGet(
                            "key1" to "field11",
                            "key2" to "field22",
                        ) shouldBe listOf("value11", null)
                    }
                }

                describe("when keys exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key1", "field11" to "value11", "field12" to "value12")
                        testKeyValueClient.hSet("key2", "field21" to "value21", "field22" to "value22")
                        testKeyValueClient.hSet("key3", "field31" to "value31")
                    }

                    it("must return values and nulls") {
                        keyValueClient.pipelineHGet(
                            "key1" to "field11",
                            "key2" to "field22",
                        ) shouldBe listOf("value11", "value22")
                    }
                }
            }

            describe("pipelineHGetAll") {
                describe("when keys doesn't exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key3", "field31" to "value31")
                    }

                    it("must return empty maps") {
                        keyValueClient.pipelineHGetAll("key1", "key2") shouldBe listOf(emptyMap(), emptyMap())
                    }
                }

                describe("when some keys exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key1", "field11" to "value11")
                        testKeyValueClient.hSet("key3", "field31" to "value31")
                    }

                    it("must return maps and empty maps") {
                        keyValueClient.pipelineHGetAll("key1", "key2") shouldBe
                            listOf(
                                mapOf("field11" to "value11"),
                                emptyMap(),
                            )
                    }
                }

                describe("when keys exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key1", "field11" to "value11")
                        testKeyValueClient.hSet("key2", "field22" to "value22")
                    }

                    it("must return empty maps") {
                        keyValueClient.pipelineHGetAll("key1", "key2") shouldBe
                            listOf(
                                mapOf("field11" to "value11"),
                                mapOf("field22" to "value22"),
                            )
                    }
                }
            }

            describe("eval") {
                describe("script that returns nothing") {
                    val scriptId = "ScriptThatReturnsNothing"

                    val script =
                        """
                        redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
                        """.trimIndent()

                    var result: Any? = null

                    beforeEach {
                        result = keyValueClient.eval<Unit>(scriptId, script, listOf("key"), "field1", "value1")
                    }

                    it("must return unit") {
                        result shouldBe Unit
                    }

                    it("must invoke script") {
                        testKeyValueClient.hGetAll("key") shouldBe
                            mapOf(
                                "field1" to "value1",
                            )
                    }
                }

                describe("script that returns long") {
                    val scriptId = "ScriptThatReturnsLong"

                    val script =
                        """
                        redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])

                        return 5
                        """.trimIndent()

                    var result: Long? = null

                    beforeEach {
                        result = keyValueClient.eval<Long>(scriptId, script, listOf("key"), "field1", "value1")
                    }

                    it("must return long") {
                        result shouldBe 5L
                    }

                    it("must invoke script") {
                        testKeyValueClient.hGetAll("key") shouldBe
                            mapOf(
                                "field1" to "value1",
                            )
                    }
                }

                describe("after SCRIPT FLUSH") {
                    val scriptId = "ScriptAfterFlush"

                    val script =
                        """
                        redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
                        """.trimIndent()

                    beforeEach {
                        keyValueClient.eval<Unit>(scriptId, script, listOf("key"), "field1", "value1")

                        testKeyValueClient.scriptFlush()

                        keyValueClient.eval<Unit>(scriptId, script, listOf("key"), "field2", "value2")
                    }

                    it("must invoke script") {
                        testKeyValueClient.hGetAll("key") shouldBe
                            mapOf(
                                "field1" to "value1",
                                "field2" to "value2",
                            )
                    }
                }
            }
        }
    }
})
