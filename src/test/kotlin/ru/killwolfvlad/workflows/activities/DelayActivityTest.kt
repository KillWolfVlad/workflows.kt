package ru.killwolfvlad.workflows.activities

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ru.killwolfvlad.workflows.core.ActivityContext
import ru.killwolfvlad.workflows.core.WorkflowContext
import ru.killwolfvlad.workflows.core.coroutines.ActivityCoroutineContext
import ru.killwolfvlad.workflows.core.coroutines.WorkflowCoroutineContext
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.types.ActivityCallback
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.withActivity
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DelayActivityTest : WorkflowsDescribeSpec({
    val now = Instant.parse("2025-01-01T00:00:00Z")

    mockkObject(Clock.System)
    mockkStatic("kotlinx.coroutines.DelayKt")

    mockkStatic("ru.killwolfvlad.workflows.core.WithActivityCoreKt")

    val workflowId = WorkflowId("workflow1")
    val activityId = "activity1"

    val activityContextMock = mockk<ActivityContext>()

    val defaultWorkflowCoroutineContext =
        WorkflowCoroutineContext(
            workflowId,
            mockk<WorkflowContext>(),
            activityContextMock,
            mockk<KeyValueClient>(),
        )

    val defaultActivityCoroutineContext = ActivityCoroutineContext(activityId)

    beforeEach {
        every {
            Clock.System.now()
        } returns now

        coJustRun { delay(any(Duration::class)) }
    }

    describe("when untilDate doesn't exists") {
        beforeEach {
            coEvery { withActivity(activityId, emptyList(), listOf("untilDate"), any()) } coAnswers {
                val block = arg<ActivityCallback>(3)

                withContext(defaultActivityCoroutineContext) {
                    block(emptyMap(), emptyMap())
                }

                Unit
            }

            coJustRun { activityContextMock.set(any()) }

            withContext(defaultWorkflowCoroutineContext) {
                delayActivity(activityId, 5.seconds)
            }
        }

        it("must save untilDate in activity context") {
            coVerify(exactly = 1) { activityContextMock.set(mapOf("untilDate" to "2025-01-01T00:00:05Z")) }
        }

        it("must call delay") {
            coVerify(exactly = 1) { delay(5.seconds) }
        }
    }

    describe("when untilDate exists") {
        beforeEach {
            coEvery { withActivity(activityId, emptyList(), listOf("untilDate"), any()) } coAnswers {
                val block = arg<ActivityCallback>(3)

                withContext(defaultActivityCoroutineContext) {
                    block(emptyMap(), mapOf("untilDate" to "2025-01-01T00:00:03Z"))
                }

                Unit
            }

            withContext(defaultWorkflowCoroutineContext) {
                delayActivity(activityId, 5.seconds)
            }
        }

        it("must call delay") {
            coVerify(exactly = 1) { delay(3.seconds) }
        }
    }
})
