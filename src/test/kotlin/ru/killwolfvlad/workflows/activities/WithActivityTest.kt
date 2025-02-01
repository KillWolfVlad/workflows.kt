package ru.killwolfvlad.workflows.activities

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import ru.killwolfvlad.workflows.activities.types.ActivityCallback
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec
import ru.killwolfvlad.workflows.core.types.ActivityCallback as CoreActivityCallback
import ru.killwolfvlad.workflows.core.withActivity as coreWithActivity

class WithActivityTest : WorkflowsDescribeSpec({
    mockkStatic(::coreWithActivity)

    val activityId = "activity1"

    val activityCallbackMock = mockk<ActivityCallback>()

    describe("when activity don't has workflow context keys") {
        beforeEach {
            coEvery { coreWithActivity(activityId, emptyList(), emptyList(), any()) } coAnswers {
                val block = arg<CoreActivityCallback>(3)

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
            coEvery { coreWithActivity(activityId, listOf("field1", "field2"), emptyList(), any()) } coAnswers {
                val block = arg<CoreActivityCallback>(3)

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
            coEvery { coreWithActivity(activityId, listOf("field1", "field2"), emptyList(), any()) } coAnswers {
                val block = arg<CoreActivityCallback>(3)

                result = block(mapOf("field1" to "value1", "field2" to "value2"), emptyMap())
            }

            coEvery { activityCallbackMock(any()) } returns mapOf("field1" to "value1", "field2" to "value2")

            withActivity(activityId, workflowContextKeys = listOf("field1", "field2"), block = activityCallbackMock)
        }

        it("must return workflow context") {
            result shouldBe mapOf("field1" to "value1", "field2" to "value2")
        }
    }
})
