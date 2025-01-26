package ru.killwolfvlad.workflows.activities

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkStatic
import ru.killwolfvlad.workflows.WorkflowsDescribeSpec
import ru.killwolfvlad.workflows.core.withActivity
import kotlin.time.Duration.Companion.seconds

@Suppress("UNCHECKED_CAST")
class DelayActivityTest : WorkflowsDescribeSpec({
    val activityId = "activity1"

    mockkStatic(::withActivity)

    beforeEach {
        coEvery { withActivity(activityId, emptyList(), listOf("untilDate"), any()) } coAnswers {
            val block = args[3] as suspend (Map<String, String?>, Map<String, String?>) -> Map<String, String>?

            block(emptyMap(), emptyMap())

            Unit
        }
    }

    it("me") {
        delayActivity(activityId, 5.seconds)

        coVerify(exactly = 1) { withActivity(activityId, emptyList(), listOf("untilDate"), any()) }
    }

    it("me2") {
        delayActivity(activityId, 5.seconds)

        coVerify(exactly = 1) { withActivity(activityId, emptyList(), listOf("untilDate"), any()) }
    }
})
