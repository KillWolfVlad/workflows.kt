package ru.killwolfvlad.workflows.activities

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ru.killwolfvlad.workflows.core.ActivityContext
import ru.killwolfvlad.workflows.core.coroutines.getActivityContext
import ru.killwolfvlad.workflows.core.withActivity
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

suspend fun delayActivity(
    activityId: String,
    duration: Duration,
) = withActivity(activityId) {
    val now = Clock.System.now()

    val activityContext = coroutineContext.getActivityContext()

    var untilDate = activityContext.getUntilDate()

    if (untilDate == null) {
        untilDate = now + duration

        activityContext.setUntilDate(untilDate)
    }

    delay(untilDate - Clock.System.now())

    null
}

private const val UNTIL_DATE_ACTIVITY_CONTEXT_KEY = "untilDate"

private suspend inline fun ActivityContext.getUntilDate(): Instant? =
    get(UNTIL_DATE_ACTIVITY_CONTEXT_KEY)?.let { Instant.parse(it) }

private suspend inline fun ActivityContext.setUntilDate(value: Instant) =
    set(mapOf(UNTIL_DATE_ACTIVITY_CONTEXT_KEY to value.toString()))
