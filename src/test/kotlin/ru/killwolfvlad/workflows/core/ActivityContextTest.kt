package ru.killwolfvlad.workflows.core

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.withContext
import ru.killwolfvlad.workflows.core.coroutines.ActivityCoroutineContext
import ru.killwolfvlad.workflows.core.coroutines.WorkflowCoroutineContext
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec

class ActivityContextTest : WorkflowsDescribeSpec({
    val keyValueClientMock = mockk<KeyValueClient>()

    val activityContext = ActivityContext(keyValueClientMock)

    val workflowId = WorkflowId("workflow1")
    val activityId = "activity1"

    val defaultWorkflowCoroutineContext =
        WorkflowCoroutineContext(
            workflowId,
            mockk<WorkflowContext>(),
            mockk<ActivityContext>(),
            mockk<KeyValueClient>(),
        )

    val defaultActivityCoroutineContext = ActivityCoroutineContext(activityId)

    val defaultCoroutineContext = defaultWorkflowCoroutineContext + defaultActivityCoroutineContext

    describe("get") {
        describe("single field") {
            beforeEach {
                coEvery { keyValueClientMock.hGet(workflowId.workflowKey, "act:activity1:ctx:field1") } returns "value1"
            }

            it("must return value") {
                withContext(defaultCoroutineContext) {
                    activityContext.get("field1") shouldBe "value1"
                }
            }

            it("must throw error if called not in workflow coroutine context") {
                shouldThrowWithMessage<NullPointerException>("WorkflowCoroutineContext must be in coroutineContext!") {
                    activityContext.get("field1")
                }
            }

            it("must throw error if called not in activity coroutine context") {
                shouldThrowWithMessage<NullPointerException>("ActivityCoroutineContext must be in coroutineContext!") {
                    withContext(defaultWorkflowCoroutineContext) {
                        activityContext.get("field1") shouldBe "value1"
                    }
                }
            }
        }

        describe("empty fields") {
            var result: Map<String, String?>? = null

            beforeEach {
                coEvery { keyValueClientMock.hMGet(any()) } returns emptyList()

                withContext(defaultCoroutineContext) {
                    result = activityContext.get()
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
                        "act:activity1:ctx:field1",
                        "act:activity1:ctx:field2",
                    )
                } returns listOf(null, "value2")
            }

            it("must return values") {
                withContext(defaultCoroutineContext) {
                    activityContext.get("field1", "field2") shouldBe
                        mapOf(
                            "field1" to null,
                            "field2" to "value2",
                        )
                }
            }

            it("must throw error if called not in workflow coroutine context") {
                shouldThrowWithMessage<NullPointerException>("WorkflowCoroutineContext must be in coroutineContext!") {
                    activityContext.get("field1", "field2")
                }
            }

            it("must throw error if called not in activity coroutine context") {
                shouldThrowWithMessage<NullPointerException>("ActivityCoroutineContext must be in coroutineContext!") {
                    withContext(defaultWorkflowCoroutineContext) {
                        activityContext.get("field1", "field2")
                    }
                }
            }
        }
    }

    describe("set") {
        describe("empty fields") {
            beforeEach {
                coJustRun { keyValueClientMock.hSet(any()) }

                withContext(defaultCoroutineContext) {
                    activityContext.set(emptyMap())
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
                        "act:activity1:ctx:field1" to "value1",
                        "act:activity1:ctx:field2" to "value2",
                    )
                }
            }

            it("must set values") {
                withContext(defaultCoroutineContext) {
                    activityContext.set(mapOf("field1" to "value1", "field2" to "value2"))
                }
            }

            it("must throw error if called not in workflow coroutine context") {
                shouldThrowWithMessage<NullPointerException>("WorkflowCoroutineContext must be in coroutineContext!") {
                    activityContext.set(mapOf("field1" to "value1", "field2" to "value2"))
                }
            }

            it("must throw error if called not in activity coroutine context") {
                shouldThrowWithMessage<NullPointerException>("ActivityCoroutineContext must be in coroutineContext!") {
                    withContext(defaultWorkflowCoroutineContext) {
                        activityContext.set(mapOf("field1" to "value1", "field2" to "value2"))
                    }
                }
            }
        }
    }

    describe("delete") {
        describe("empty fields") {
            beforeEach {
                coJustRun { keyValueClientMock.hDel(any()) }

                withContext(defaultCoroutineContext) {
                    activityContext.delete()
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
                        "act:activity1:ctx:field1",
                        "act:activity1:ctx:field2",
                    )
                }
            }

            it("must delete values") {
                withContext(defaultCoroutineContext) {
                    activityContext.delete("field1", "field2")
                }
            }

            it("must throw error if called not in workflow coroutine context") {
                shouldThrowWithMessage<NullPointerException>("WorkflowCoroutineContext must be in coroutineContext!") {
                    activityContext.delete("field1", "field2")
                }
            }

            it("must throw error if called not in activity coroutine context") {
                shouldThrowWithMessage<NullPointerException>("ActivityCoroutineContext must be in coroutineContext!") {
                    withContext(defaultWorkflowCoroutineContext) {
                        activityContext.delete("field1", "field2")
                    }
                }
            }
        }
    }

    describe("getActivityContextFieldKey") {
        it("must return activity context field key") {
            withContext(defaultActivityCoroutineContext) {
                "field1".getActivityContextFieldKey() shouldBe "act:activity1:ctx:field1"
            }
        }

        it("must throw error if called not in activity coroutine context") {
            shouldThrowWithMessage<NullPointerException>("ActivityCoroutineContext must be in coroutineContext!") {
                "field1".getActivityContextFieldKey()
            }
        }
    }
})
