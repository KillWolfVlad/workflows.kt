package ru.killwolfvlad.workflows.core

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import ru.killwolfvlad.workflows.core.types.ActivityCallback
import ru.killwolfvlad.workflows.core.types.SimpleActivityCallback
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec

class WithActivityTest : WorkflowsDescribeSpec({
    mockkStatic("ru.killwolfvlad.workflows.core.WithActivityCoreKt")

    val activityId = "activity1"

    describe("simple activity") {
        val activityCallbackMock = mockk<SimpleActivityCallback>()

        describe("when activity don't has workflow context keys") {
            beforeEach {
                coEvery { withActivity(activityId, emptyList(), emptyList(), any()) } coAnswers {
                    val block = arg<ActivityCallback>(3)

                    block(emptyMap(), emptyMap())

                    Unit
                }

                coEvery { activityCallbackMock(any()) } returns null

                withActivity(activityId, block = activityCallbackMock)
            }

            it("must call activity callback with empty workflow context map") {
                coVerify(exactly = 1) { activityCallbackMock(emptyMap()) }
            }
        }

        describe("when activity has workflow context keys") {
            beforeEach {
                coEvery { withActivity(activityId, listOf("field1", "field2"), emptyList(), any()) } coAnswers {
                    val block = arg<ActivityCallback>(3)

                    block(mapOf("field1" to "value1", "field2" to "value2"), emptyMap())

                    Unit
                }

                coEvery { activityCallbackMock(any()) } returns null

                withActivity(activityId, workflowContextKeys = listOf("field1", "field2"), block = activityCallbackMock)
            }

            it("must call activity callback with workflow context map") {
                coVerify(exactly = 1) { activityCallbackMock(mapOf("field1" to "value1", "field2" to "value2")) }
            }
        }

        describe("when activity callback returns workflow context") {
            var result: Map<String, String>? = null

            beforeEach {
                coEvery { withActivity(activityId, listOf("field1", "field2"), emptyList(), any()) } coAnswers {
                    val block = arg<ActivityCallback>(3)

                    result = block(mapOf("field1" to "value1", "field2" to "value2"), emptyMap())
                }

                coEvery { activityCallbackMock(any()) } returns mapOf("field1" to "value1", "field2" to "value2")

                withActivity(activityId, workflowContextKeys = listOf("field1", "field2"), block = activityCallbackMock)
            }

            it("must return workflow context") {
                result shouldBe mapOf("field1" to "value1", "field2" to "value2")
            }
        }
    }
})
