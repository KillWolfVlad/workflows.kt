package ru.killwolfvlad.workflows.core

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import ru.killwolfvlad.workflows.WorkflowsDescribeSpec
import ru.killwolfvlad.workflows.core.coroutines.WorkflowCoroutineContext
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.internal.enums.ActivityStatus
import ru.killwolfvlad.workflows.core.internal.enums.WorkflowSignal
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey

class WithActivityTest : WorkflowsDescribeSpec({
    val keyValueClientMock = mockk<KeyValueClient>()
    val activityCallbackMock = mockk<suspend (Map<String, String?>, Map<String, String?>) -> Map<String, String>?>()
    val liteActivityCallbackMock = mockk<suspend (Map<String, String?>) -> Map<String, String>?>()

    val workflowId = WorkflowId("workflow1")
    val activityId = "activity1"

    val defaultCoroutineContext = WorkflowCoroutineContext(
        workflowId,
        mockk<WorkflowContext>(),
        mockk<ActivityContext>(),
        workflowId.workflowKey,
        keyValueClientMock,
    )

    describe("when workflow has CANCEL signal") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                )
            } returns listOf(WorkflowSignal.CANCEL.toString(), null)

            coEvery { activityCallbackMock(any(), any()) } returns null
        }

        it("must throw cancellation exception") {
            shouldThrow<CancellationException> {
                withContext(defaultCoroutineContext) {
                    withActivity(activityId, block = activityCallbackMock)
                }
            }
        }

        it("must don't call activity callback") {
            runCatching {
                withContext(defaultCoroutineContext) {
                    withActivity(activityId, block = activityCallbackMock)
                }
            }

            coVerify(exactly = 0) { activityCallbackMock(any(), any()) }
        }
    }

    describe("when workflow has CANCEL signal on lite version") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                )
            } returns listOf(WorkflowSignal.CANCEL.toString(), null)

            coEvery { liteActivityCallbackMock(any()) } returns null
        }

        it("must throw cancellation exception") {
            shouldThrow<CancellationException> {
                withContext(defaultCoroutineContext) {
                    withActivity(activityId, block = liteActivityCallbackMock)
                }
            }
        }

        it("must don't call activity callback") {
            runCatching {
                withContext(defaultCoroutineContext) {
                    withActivity(activityId, block = liteActivityCallbackMock)
                }
            }

            coVerify(exactly = 0) { liteActivityCallbackMock(any()) }
        }
    }

    describe("when activity has COMPLETED status") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                )
            } returns listOf(null, ActivityStatus.COMPLETED.toString())

            coEvery { activityCallbackMock(any(), any()) } returns null

            withContext(defaultCoroutineContext) {
                withActivity(activityId, block = activityCallbackMock)
            }
        }

        it("must do nothing") {
            coVerify(exactly = 0) { activityCallbackMock(any(), any()) }
        }
    }

    describe("when activity has COMPLETED status on lite version") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                )
            } returns listOf(null, ActivityStatus.COMPLETED.toString())

            coEvery { liteActivityCallbackMock(any()) } returns null

            withContext(defaultCoroutineContext) {
                withActivity(activityId, block = liteActivityCallbackMock)
            }
        }

        it("must do nothing") {
            coVerify(exactly = 0) { liteActivityCallbackMock(any()) }
        }
    }

    describe("when activity don't has workflow and activity context keys") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                )
            } returns listOf(null, null)

            coJustRun {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }

            coEvery {
                activityCallbackMock(
                    emptyMap(),
                    emptyMap(),
                )
            } returns null

            withContext(defaultCoroutineContext) {
                withActivity(
                    activityId,
                    block = activityCallbackMock,
                )
            }
        }

        it("must call activity callback") {
            coVerify(exactly = 1) {
                activityCallbackMock(
                    emptyMap(),
                    emptyMap(),
                )
            }
        }

        it("must set COMPLETED status for activity") {
            coVerify(exactly = 1) {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }
        }
    }

    describe("when activity don't has workflow keys on lite version") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                )
            } returns listOf(null, null)

            coJustRun {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }

            coEvery {
                liteActivityCallbackMock(emptyMap())
            } returns null

            withContext(defaultCoroutineContext) {
                withActivity(
                    activityId,
                    block = liteActivityCallbackMock,
                )
            }
        }

        it("must call activity callback") {
            coVerify(exactly = 1) {
                liteActivityCallbackMock(emptyMap())
            }
        }

        it("must set COMPLETED status for activity") {
            coVerify(exactly = 1) {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }
        }
    }

    describe("when activity has only workflow context keys") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                    "ctx:field1",
                    "ctx:field2",
                )
            } returns listOf(null, null, "value1", "value2")

            coJustRun {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }

            coEvery {
                activityCallbackMock(
                    mapOf("field1" to "value1", "field2" to "value2"),
                    emptyMap(),
                )
            } returns null

            withContext(defaultCoroutineContext) {
                withActivity(
                    activityId,
                    workflowContextKeys = listOf("field1", "field2"),
                    block = activityCallbackMock,
                )
            }
        }

        it("must call activity callback") {
            coVerify(exactly = 1) {
                activityCallbackMock(
                    mapOf("field1" to "value1", "field2" to "value2"),
                    emptyMap(),
                )
            }
        }

        it("must set COMPLETED status for activity") {
            coVerify(exactly = 1) {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }
        }
    }

    describe("when activity has workflow context keys on lite version") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                    "ctx:field1",
                    "ctx:field2",
                )
            } returns listOf(null, null, "value1", "value2")

            coJustRun {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }

            coEvery {
                liteActivityCallbackMock(mapOf("field1" to "value1", "field2" to "value2"))
            } returns null

            withContext(defaultCoroutineContext) {
                withActivity(
                    activityId,
                    workflowContextKeys = listOf("field1", "field2"),
                    block = liteActivityCallbackMock,
                )
            }
        }

        it("must call activity callback") {
            coVerify(exactly = 1) {
                liteActivityCallbackMock(mapOf("field1" to "value1", "field2" to "value2"))
            }
        }

        it("must set COMPLETED status for activity") {
            coVerify(exactly = 1) {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }
        }
    }

    describe("when activity has only activity context keys") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                    "act:activity1:ctx:field1",
                    "act:activity1:ctx:field2",
                )
            } returns listOf(null, null, "value1", "value2")

            coJustRun {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }

            coEvery {
                activityCallbackMock(
                    emptyMap(),
                    mapOf("field1" to "value1", "field2" to "value2"),
                )
            } returns null

            withContext(defaultCoroutineContext) {
                withActivity(
                    activityId,
                    activityContextKeys = listOf("field1", "field2"),
                    block = activityCallbackMock,
                )
            }
        }

        it("must call activity callback") {
            coVerify(exactly = 1) {
                activityCallbackMock(
                    emptyMap(),
                    mapOf("field1" to "value1", "field2" to "value2"),
                )
            }
        }

        it("must set COMPLETED status for activity") {
            coVerify(exactly = 1) {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                )
            }
        }
    }

    describe("when activity callback returns workflow context") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                )
            } returns listOf(null, null)

            coJustRun {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                    "ctx:field1" to "value1",
                    "ctx:field2" to "value2",
                )
            }

            coEvery {
                activityCallbackMock(
                    emptyMap(),
                    emptyMap()
                )
            } returns mapOf("field1" to "value1", "field2" to "value2")

            withContext(defaultCoroutineContext) {
                withActivity(
                    activityId,
                    block = activityCallbackMock,
                )
            }
        }

        it("must call activity callback") {
            coVerify(exactly = 1) {
                activityCallbackMock(
                    emptyMap(),
                    emptyMap(),
                )
            }
        }

        it("must set COMPLETED status for activity and returned workflow context") {
            coVerify(exactly = 1) {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                    "ctx:field1" to "value1",
                    "ctx:field2" to "value2",
                )
            }
        }
    }

    describe("when activity callback returns workflow context on lite version") {
        beforeEach {
            coEvery {
                keyValueClientMock.hMGet(
                    workflowId.workflowKey,
                    WorkflowSignal.FIELD_KEY,
                    "act:activity1:status",
                )
            } returns listOf(null, null)

            coJustRun {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                    "ctx:field1" to "value1",
                    "ctx:field2" to "value2",
                )
            }

            coEvery {
                liteActivityCallbackMock(emptyMap())
            } returns mapOf("field1" to "value1", "field2" to "value2")

            withContext(defaultCoroutineContext) {
                withActivity(
                    activityId,
                    block = liteActivityCallbackMock,
                )
            }
        }

        it("must call activity callback") {
            coVerify(exactly = 1) {
                liteActivityCallbackMock(emptyMap())
            }
        }

        it("must set COMPLETED status for activity and returned workflow context") {
            coVerify(exactly = 1) {
                keyValueClientMock.hSet(
                    workflowId.workflowKey,
                    "act:activity1:status" to ActivityStatus.COMPLETED.toString(),
                    "ctx:field1" to "value1",
                    "ctx:field2" to "value2",
                )
            }
        }
    }
})
