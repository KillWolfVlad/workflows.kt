package ru.killwolfvlad.workflows.core.internal

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_CLASS_NAME_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_LOCKS_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_WORKERS_KEY
import ru.killwolfvlad.workflows.core.internal.extensions.workflowClassName
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec

class LuaScriptsTest : WorkflowsDescribeSpec({
    for (testClient in testClients) {
        val keyValueClient = testClient.keyValueClient
        val testKeyValueClient = testClient.testKeyValueClient

        describe(testClient.name) {
            beforeEach {
                testKeyValueClient.flushDB()
            }

            describe("heartbeat") {
                describe("when heartbeat doesn't exists") {
                    beforeEach {
                        keyValueClient.heartbeat(
                            WORKFLOW_WORKERS_KEY,
                            defaultWorkflowsConfig.workerId,
                            0,
                            defaultWorkflowsConfig.lockTimeout,
                        )
                    }

                    it("must set heartbeat") {
                        testKeyValueClient.hGet(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId) shouldBe "0"
                    }

                    it("must update heartbeat ttl") {
                        val ttl =
                            testKeyValueClient.hPTTL(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId).first()

                        ttl shouldBeInRange 0L..defaultWorkflowsConfig.lockTimeout.inWholeMilliseconds
                    }
                }

                describe("when heartbeat exists") {
                    beforeEach {
                        testKeyValueClient.hSet(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId to "5")

                        keyValueClient.heartbeat(
                            WORKFLOW_WORKERS_KEY,
                            defaultWorkflowsConfig.workerId,
                            10,
                            defaultWorkflowsConfig.lockTimeout,
                        )
                    }

                    it("must set heartbeat") {
                        testKeyValueClient.hGet(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId) shouldBe "10"
                    }

                    it("must update heartbeat ttl") {
                        val ttl =
                            testKeyValueClient.hPTTL(WORKFLOW_WORKERS_KEY, defaultWorkflowsConfig.workerId).first()

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
                        result =
                            keyValueClient.acquireWorkflowLock(
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
                        testKeyValueClient.hGetAll(currentWorkflowId.workflowKey) shouldBe
                            mapOf(
                                WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                "field1" to "value1",
                                "field2" to "value2",
                            )
                    }

                    it("must set lock") {
                        testKeyValueClient.hGet(WORKFLOW_LOCKS_KEY, currentWorkflowId.value) shouldBe currentWorkerId
                    }
                }

                describe("when workflow exists") {
                    beforeEach {
                        testKeyValueClient.hSet(
                            currentWorkflowId.workflowKey,
                            WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                            "field1" to "value1",
                            "field2" to "value2",
                        )
                    }

                    describe("when lock acquired by current worker") {
                        var result: Long? = null

                        beforeEach {
                            testKeyValueClient.hSet(
                                WORKFLOW_LOCKS_KEY,
                                currentWorkflowId.value to currentWorkerId,
                            )

                            result =
                                keyValueClient.acquireWorkflowLock(
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
                            testKeyValueClient.hGetAll(currentWorkflowId.workflowKey) shouldBe
                                mapOf(
                                    WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                    "field1" to "value1",
                                    "field2" to "value2",
                                )
                        }

                        it("must don't change lock") {
                            testKeyValueClient.hGet(
                                WORKFLOW_LOCKS_KEY,
                                currentWorkflowId.value,
                            ) shouldBe currentWorkerId
                        }
                    }

                    describe("when lock acquired by other worker") {
                        beforeEach {
                            testKeyValueClient.hSet(WORKFLOW_LOCKS_KEY, currentWorkflowId.value to otherWorkerId)
                        }

                        describe("when other worker doesn't exists") {
                            var result: Long? = null

                            beforeEach {
                                result =
                                    keyValueClient.acquireWorkflowLock(
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
                                testKeyValueClient.hGetAll(currentWorkflowId.workflowKey) shouldBe
                                    mapOf(
                                        WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                        "field1" to "value1",
                                        "field2" to "value2",
                                    )
                            }

                            it("must change lock to current worker") {
                                testKeyValueClient.hGet(
                                    WORKFLOW_LOCKS_KEY,
                                    currentWorkflowId.value,
                                ) shouldBe currentWorkerId
                            }
                        }

                        describe("when other worker exists") {
                            var result: Long? = null

                            beforeEach {
                                testKeyValueClient.hSet(WORKFLOW_WORKERS_KEY, otherWorkerId to "1")

                                result =
                                    keyValueClient.acquireWorkflowLock(
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
                                testKeyValueClient.hGetAll(currentWorkflowId.workflowKey) shouldBe
                                    mapOf(
                                        WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                        "field1" to "value1",
                                        "field2" to "value2",
                                    )
                            }

                            it("must don't change lock") {
                                testKeyValueClient.hGet(
                                    WORKFLOW_LOCKS_KEY,
                                    currentWorkflowId.value,
                                ) shouldBe otherWorkerId
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
                        testKeyValueClient.hGetAll(otherWorkflowId.workflowKey) shouldBe
                            mapOf(
                                WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                "field1" to "value1",
                                "field2" to "value2",
                            )
                    }

                    it("must don't change other locks") {
                        testKeyValueClient.hGet(WORKFLOW_LOCKS_KEY, otherWorkflowId.value) shouldBe otherWorkerId
                    }
                }

                describe("when workflow exists") {
                    var result: Long? = null

                    beforeEach {
                        result =
                            keyValueClient.acquireWorkflowLock(
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
                        testKeyValueClient.exists(currentWorkflowId.workflowKey) shouldBe 0L
                    }

                    it("must delete lock") {
                        testKeyValueClient.hExists(WORKFLOW_LOCKS_KEY, currentWorkflowId.value) shouldBe false
                    }

                    it("must don't change other workflows") {
                        testKeyValueClient.hGetAll(otherWorkflowId.workflowKey) shouldBe
                            mapOf(
                                WORKFLOW_CLASS_NAME_FIELD_KEY to LuaScriptsTest::class.workflowClassName,
                                "field1" to "value1",
                                "field2" to "value2",
                            )
                    }

                    it("must don't change other locks") {
                        testKeyValueClient.hGet(WORKFLOW_LOCKS_KEY, otherWorkflowId.value) shouldBe otherWorkerId
                    }
                }
            }

            describe("hSetIfKeyExistsScript") {
                describe("when key doesn't exist") {
                    beforeEach {
                        keyValueClient.hSetIfKeyExistsScript("key", "field1" to "value1", "field2" to "value2")
                    }

                    it("must do nothing") {
                        testKeyValueClient.keys("*").toList() shouldHaveSize 0
                    }
                }

                describe("when key exists") {
                    beforeEach {
                        testKeyValueClient.hSet("key", "field1" to "value1", "field2" to "value2")
                    }

                    describe("when fields doesn't exists") {
                        beforeEach {
                            keyValueClient.hSetIfKeyExistsScript("key", "field3" to "value3", "field4" to "value4")
                        }

                        it("must add values") {
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
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
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
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
                            testKeyValueClient.hGetAll("key") shouldBe
                                mapOf(
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
