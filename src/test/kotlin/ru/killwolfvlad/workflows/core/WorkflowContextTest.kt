package ru.killwolfvlad.workflows.core

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.withContext
import ru.killwolfvlad.workflows.core.coroutines.WorkflowCoroutineContext
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec

class WorkflowContextTest : WorkflowsDescribeSpec({
    val keyValueClientMock = mockk<KeyValueClient>()

    val workflowContext = WorkflowContext(keyValueClientMock)

    val workflowId = WorkflowId("workflow1")

    val defaultCoroutineContext =
        WorkflowCoroutineContext(
            workflowId,
            mockk<WorkflowContext>(),
            mockk<ActivityContext>(),
            mockk<KeyValueClient>(),
        )

    describe("get") {
        describe("single field") {
            beforeEach {
                coEvery { keyValueClientMock.hGet(workflowId.workflowKey, "ctx:field1") } returns "value1"
            }

            it("must return value") {
                withContext(defaultCoroutineContext) {
                    workflowContext.get("field1") shouldBe "value1"
                }
            }

            it("must throw error if called not in workflow coroutine context") {
                shouldThrowWithMessage<NullPointerException>("WorkflowCoroutineContext must be in coroutineContext!") {
                    workflowContext.get("field1")
                }
            }
        }

        describe("empty fields") {
            var result: Map<String, String?>? = null

            beforeEach {
                coEvery { keyValueClientMock.hMGet(any()) } returns emptyList()

                withContext(defaultCoroutineContext) {
                    result = workflowContext.get()
                }
            }

            it("must return empty map") {
                result shouldBe emptyMap()
            }

            it("must don't call client") {
                coVerify(exactly = 0) { keyValueClientMock.hMGet(any()) }
            }
        }

        describe("multiple fields") {
            beforeEach {
                coEvery {
                    keyValueClientMock.hMGet(
                        workflowId.workflowKey,
                        "ctx:field1",
                        "ctx:field2",
                    )
                } returns listOf("value1", null)
            }

            it("must return values") {
                withContext(defaultCoroutineContext) {
                    workflowContext.get("field1", "field2") shouldBe
                        mapOf(
                            "field1" to "value1",
                            "field2" to null,
                        )
                }
            }

            it("must throw error if called not in workflow coroutine context") {
                shouldThrowWithMessage<NullPointerException>("WorkflowCoroutineContext must be in coroutineContext!") {
                    workflowContext.get("field1", "field2")
                }
            }
        }
    }

    describe("set") {
        describe("empty fields") {
            beforeEach {
                coJustRun { keyValueClientMock.hSet(any()) }

                withContext(defaultCoroutineContext) {
                    workflowContext.set(emptyMap())
                }
            }

            it("must don't call client") {
                coVerify(exactly = 0) { keyValueClientMock.hSet(any()) }
            }
        }

        describe("multiple fields") {
            beforeEach {
                coJustRun {
                    keyValueClientMock.hSet(
                        workflowId.workflowKey,
                        "ctx:field1" to "value1",
                        "ctx:field2" to "value2",
                    )
                }
            }

            it("must set values") {
                withContext(defaultCoroutineContext) {
                    workflowContext.set(mapOf("field1" to "value1", "field2" to "value2"))
                }
            }

            it("must throw error if called not in workflow coroutine context") {
                shouldThrowWithMessage<NullPointerException>("WorkflowCoroutineContext must be in coroutineContext!") {
                    workflowContext.set(mapOf("field1" to "value1", "field2" to "value2"))
                }
            }
        }
    }

    describe("delete") {
        describe("empty fields") {
            beforeEach {
                coJustRun { keyValueClientMock.hDel(any()) }

                withContext(defaultCoroutineContext) {
                    workflowContext.delete()
                }
            }

            it("must don't call client") {
                coVerify(exactly = 0) { keyValueClientMock.hDel(any()) }
            }
        }

        describe("multiple fields") {
            beforeEach {
                coJustRun {
                    keyValueClientMock.hDel(
                        workflowId.workflowKey,
                        "ctx:field1",
                        "ctx:field2",
                    )
                }
            }

            it("must delete values") {
                withContext(defaultCoroutineContext) {
                    workflowContext.delete("field1", "field2")
                }
            }

            it("must throw error if called not in workflow coroutine context") {
                shouldThrowWithMessage<NullPointerException>("WorkflowCoroutineContext must be in coroutineContext!") {
                    workflowContext.delete("field1", "field2")
                }
            }
        }
    }

    describe("workflowContextFieldKey") {
        it("must return workflow context field key") {
            "field1".workflowContextFieldKey shouldBe "ctx:field1"
        }
    }
})
