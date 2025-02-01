package ru.killwolfvlad.workflows.activities

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.coroutines.getActivityContext
import ru.killwolfvlad.workflows.core.withActivity
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

private const val UNTIL_DATE_ACTIVITY_CONTEXT_KEY = "untilDate"

@OptIn(WorkflowsPerformance::class)
suspend fun delayActivity(
    activityId: String,
    duration: Duration,
) = withActivity(
    activityId,
    activityContextKeys = listOf(UNTIL_DATE_ACTIVITY_CONTEXT_KEY),
) { _, activityContextMap ->
    var untilDate =
        activityContextMap[UNTIL_DATE_ACTIVITY_CONTEXT_KEY]?.let {
            Instant.parse(it)
        }

    if (untilDate == null) {
        untilDate = Clock.System.now() + duration

        coroutineContext.getActivityContext().set(mapOf(UNTIL_DATE_ACTIVITY_CONTEXT_KEY to untilDate.toString()))
    }

    delay(untilDate - Clock.System.now())

    null
}
