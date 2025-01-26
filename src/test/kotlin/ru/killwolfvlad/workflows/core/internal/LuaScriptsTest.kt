package ru.killwolfvlad.workflows.core.internal

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import ru.killwolfvlad.workflows.WorkflowsDescribeSpec
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_CLASS_NAME_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_LOCKS_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_WORKERS_KEY
import ru.killwolfvlad.workflows.core.internal.extensions.workflowClassName
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey
import ru.killwolfvlad.workflows.hgetallAsMap

class LuaScriptsTest : WorkflowsDescribeSpec({
    for (testClient in testClients) {
        val rawClient = testClient.rawClient
        val keyValueClient = testClient.keyValueClient

        describe(testClient.name) {
            beforeEach {
                rawClient.flushdb()
            }

            describe("heartbeat") {
                describe("when heartbeat doesn't exists") {
                    beforeEach {
                        keyValueClient.heartbeat(
                            WORKFLOW_WORKERS_KEY,
                            defaultWorkflowsConfig.workerId,
                            defaultWorkflowsConfig.lockTimeout,
                        )
                    }

                    it("must set heartbeat") {
                        rawClient.hget(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId) shouldBe "OK"
                    }

                    it("must update heartbeat ttl") {
                        val ttl = rawClient.hpttl(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId).first()

                        ttl shouldBeInRange 0L..defaultWorkflowsConfig.lockTimeout.inWholeMilliseconds
                    }
                }

                describe("when heartbeat exists") {
                    beforeEach {
                        rawClient.hset(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId, "OK")

                        keyValueClient.heartbeat(
                            WORKFLOW_WORKERS_KEY,
                            defaultWorkflowsConfig.workerId,
                            defaultWorkflowsConfig.lockTimeout,
                        )
                    }

                    it("must set heartbeat") {
                        rawClient.hget(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId) shouldBe "OK"
                    }

                    it("must update heartbeat ttl") {
                        val ttl = rawClient.hpttl(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId).first()

                        ttl shouldBeInRange 0L..defaultWorkflowsConfig.lockTimeout.inWholeMilliseconds
                    }
                }
            }

            describe("acquireWorkflowLock") {
                val currentWorkflowId = WorkflowId("workflow1")
                val currentWorkerId = "test-worker-1"
                val otherWorkerId = "test-worker-2"

                describe("when workflow doesn't exists") {
                    var result: Long? = null

                    beforeEach {
                        result = keyValueClient.acquireWorkflowLock(
                            // keys
                            workflowKey = currentWorkflowId.workflowKey,
                            workflowLocksKey = WORKFLOW_LOCKS_KEY,
                            workflowWorkersKey = WORKFLOW_WORKERS_KEY,
                            // arguments
                            workflowId = currentWorkflowId,
                            workerId = currentWorkerId,
                            // workflow context
                            workflowClassNameFieldKey = WORKFLOW_CLASS_NAME_FIELD_KEY,
                            workflowClassName = LuaScriptsTest::class.workflowClassName,
                            initialContext = mapOf("field1" to "value1", "field2" to "value2"),
                        )
                    }

                    it("must return 1") {
                        result shouldBe 1L
                    }

                    it("must set workflow") {
                        rawClient.hgetallAsMap(currentWorkflowId.workflowKey) shouldBe mapOf(
                            WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                            "field1" to "value1",
                            "field2" to "value2",
                        )
                    }

                    it("must set lock") {
                        rawClient.hget(WORKFLOW_LOCKS_KEY, currentWorkflowId.value) shouldBe currentWorkerId
                    }
                }

                describe("when workflow exists") {
                    beforeEach {
                        rawClient.hset(
                            currentWorkflowId.workflowKey, mapOf(
                                WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                "field1" to "value1",
                                "field2" to "value2",
                            )
                        )
                    }

                    describe("when lock acquired by current worker") {
                        var result: Long? = null

                        beforeEach {
                            rawClient.hset(WORKFLOW_LOCKS_KEY, mapOf(currentWorkflowId.value to currentWorkerId))

                            result = keyValueClient.acquireWorkflowLock(
                                // keys
                                workflowKey = currentWorkflowId.workflowKey,
                                workflowLocksKey = WORKFLOW_LOCKS_KEY,
                                workflowWorkersKey = WORKFLOW_WORKERS_KEY,
                                // arguments
                                workflowId = currentWorkflowId,
                                workerId = currentWorkerId,
                                // workflow context
                                workflowClassNameFieldKey = WORKFLOW_CLASS_NAME_FIELD_KEY,
                                workflowClassName = LuaScriptsTest::class.workflowClassName,
                                initialContext = mapOf("field3" to "value3", "field4" to "value4"),
                            )
                        }

                        it("must return 2") {
                            result shouldBe 2L
                        }

                        it("must don't change workflow") {
                            rawClient.hgetallAsMap(currentWorkflowId.workflowKey) shouldBe mapOf(
                                WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                "field1" to "value1",
                                "field2" to "value2",
                            )
                        }

                        it("must don't change lock") {
                            rawClient.hget(WORKFLOW_LOCKS_KEY, currentWorkflowId.value) shouldBe currentWorkerId
                        }
                    }

                    describe("when lock acquired by other worker") {
                        beforeEach {
                            rawClient.hset(WORKFLOW_LOCKS_KEY, mapOf(currentWorkflowId.value to otherWorkerId))
                        }

                        describe("when other worker doesn't exists") {
                            var result: Long? = null

                            beforeEach {
                                result = keyValueClient.acquireWorkflowLock(
                                    // keys
                                    workflowKey = currentWorkflowId.workflowKey,
                                    workflowLocksKey = WORKFLOW_LOCKS_KEY,
                                    workflowWorkersKey = WORKFLOW_WORKERS_KEY,
                                    // arguments
                                    workflowId = currentWorkflowId,
                                    workerId = currentWorkerId,
                                    // workflow context
                                    workflowClassNameFieldKey = WORKFLOW_CLASS_NAME_FIELD_KEY,
                                    workflowClassName = LuaScriptsTest::class.workflowClassName,
                                    initialContext = mapOf("field3" to "value3", "field4" to "value4"),
                                )
                            }

                            it("must return 3") {
                                result shouldBe 3L
                            }

                            it("must don't change workflow") {
                                rawClient.hgetallAsMap(currentWorkflowId.workflowKey) shouldBe mapOf(
                                    WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                    "field1" to "value1",
                                    "field2" to "value2",
                                )
                            }

                            it("must change lock to current worker") {
                                rawClient.hget(WORKFLOW_LOCKS_KEY, currentWorkflowId.value) shouldBe currentWorkerId
                            }
                        }

                        describe("when other worker exists") {
                            var result: Long? = null

                            beforeEach {
                                rawClient.hset(WORKFLOW_WORKERS_KEY, otherWorkerId, "OK")

                                result = keyValueClient.acquireWorkflowLock(
                                    // keys
                                    workflowKey = currentWorkflowId.workflowKey,
                                    workflowLocksKey = WORKFLOW_LOCKS_KEY,
                                    workflowWorkersKey = WORKFLOW_WORKERS_KEY,
                                    // arguments
                                    workflowId = currentWorkflowId,
                                    workerId = currentWorkerId,
                                    // workflow context
                                    workflowClassNameFieldKey = WORKFLOW_CLASS_NAME_FIELD_KEY,
                                    workflowClassName = LuaScriptsTest::class.workflowClassName,
                                    initialContext = mapOf("field3" to "value3", "field4" to "value4"),
                                )
                            }

                            it("must return 0") {
                                result shouldBe 0L
                            }

                            it("must don't change workflow") {
                                rawClient.hgetallAsMap(currentWorkflowId.workflowKey) shouldBe mapOf(
                                    WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                    "field1" to "value1",
                                    "field2" to "value2",
                                )
                            }

                            it("must don't change lock") {
                                rawClient.hget(WORKFLOW_LOCKS_KEY, currentWorkflowId.value) shouldBe otherWorkerId
                            }
                        }
                    }
                }
            }

            describe("deleteWorkflow") {
                val currentWorkflowId = WorkflowId("workflow1")
                val otherWorkflowId = WorkflowId("workflow2")
                val currentWorkerId = "test-worker-1"
                val otherWorkerId = "test-worker-2"

                beforeEach {
                    keyValueClient.acquireWorkflowLock(
                        // keys
                        workflowKey = otherWorkflowId.workflowKey,
                        workflowLocksKey = WORKFLOW_LOCKS_KEY,
                        workflowWorkersKey = WORKFLOW_WORKERS_KEY,
                        // arguments
                        workflowId = otherWorkflowId,
                        workerId = otherWorkerId,
                        // workflow context
                        workflowClassNameFieldKey = WORKFLOW_CLASS_NAME_FIELD_KEY,
                        workflowClassName = LuaScriptsTest::class.workflowClassName,
                        initialContext = mapOf("field1" to "value1", "field2" to "value2"),
                    )
                }

                describe("when workflow doesn't exists") {
                    beforeEach {
                        keyValueClient.deleteWorkflow(
                            // keys
                            workflowKey = currentWorkflowId.workflowKey,
                            workflowLocksKey = WORKFLOW_LOCKS_KEY,
                            // arguments
                            workflowId = currentWorkflowId,
                        )
                    }

                    it("must don't change other workflows") {
                        rawClient.hgetallAsMap(otherWorkflowId.workflowKey) shouldBe mapOf(
                            WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                            "field1" to "value1",
                            "field2" to "value2",
                        )
                    }

                    it("must don't change other locks") {
                        rawClient.hget(WORKFLOW_LOCKS_KEY, otherWorkflowId.value) shouldBe otherWorkerId
                    }
                }

                describe("when workflow exists") {
                    var result: Long? = null

                    beforeEach {
                        result = keyValueClient.acquireWorkflowLock(
                            // keys
                            workflowKey = currentWorkflowId.workflowKey,
                            workflowLocksKey = WORKFLOW_LOCKS_KEY,
                            workflowWorkersKey = WORKFLOW_WORKERS_KEY,
                            // arguments
                            workflowId = currentWorkflowId,
                            workerId = currentWorkerId,
                            // workflow context
                            workflowClassNameFieldKey = WORKFLOW_CLASS_NAME_FIELD_KEY,
                            workflowClassName = LuaScriptsTest::class.workflowClassName,
                            initialContext = mapOf("field1" to "value1", "field2" to "value2"),
                        )

                        keyValueClient.deleteWorkflow(
                            // keys
                            workflowKey = currentWorkflowId.workflowKey,
                            workflowLocksKey = WORKFLOW_LOCKS_KEY,
                            // arguments
                            workflowId = currentWorkflowId,
                        )
                    }

                    it("must set workflow before deletion") {
                        result shouldBe 1L
                    }

                    it("must delete workflow") {
                        rawClient.exists(currentWorkflowId.workflowKey) shouldBe 0L
                    }

                    it("must delete lock") {
                        rawClient.hexists(WORKFLOW_LOCKS_KEY, currentWorkflowId.value) shouldBe false
                    }

                    it("must don't change other workflows") {
                        rawClient.hgetallAsMap(otherWorkflowId.workflowKey) shouldBe mapOf(
                            WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                            "field1" to "value1",
                            "field2" to "value2",
                        )
                    }

                    it("must don't change other locks") {
                        rawClient.hget(WORKFLOW_LOCKS_KEY, otherWorkflowId.value) shouldBe otherWorkerId
                    }
                }
            }

            describe("hSetIfKeyExistsScript") {
                describe("when key doesn't exist") {
                    beforeEach {
                        keyValueClient.hSetIfKeyExistsScript("key", "field1" to "value1", "field2" to "value2")
                    }

                    it("must do nothing") {
                        rawClient.keys("*").toList() shouldHaveSize 0
                    }
                }

                describe("when key exists") {
                    beforeEach {
                        rawClient.hset("key", mapOf("field1" to "value1", "field2" to "value2"))
                    }

                    describe("when fields doesn't exists") {
                        beforeEach {
                            keyValueClient.hSetIfKeyExistsScript("key", "field3" to "value3", "field4" to "value4")
                        }

                        it("must add values") {
                            rawClient.hgetallAsMap("key") shouldBe mapOf(
                                "field1" to "value1",
                                "field2" to "value2",
                                "field3" to "value3",
                                "field4" to "value4",
                            )
                        }
                    }

                    describe("when some fields exists") {
                        beforeEach {
                            keyValueClient.hSetIfKeyExistsScript("key", "field1" to "value11", "field4" to "value4")
                        }

                        it("must add and overwrite values") {
                            rawClient.hgetallAsMap("key") shouldBe mapOf(
                                "field1" to "value11",
                                "field2" to "value2",
                                "field4" to "value4",
                            )
                        }
                    }

                    describe("when fields exists") {
                        beforeEach {
                            keyValueClient.hSetIfKeyExistsScript("key", "field1" to "value11", "field2" to "value22")
                        }

                        it("must overwrite values") {
                            rawClient.hgetallAsMap("key") shouldBe mapOf(
                                "field1" to "value11",
                                "field2" to "value22",
                            )
                        }
                    }
                }
            }
        }
    }
})
