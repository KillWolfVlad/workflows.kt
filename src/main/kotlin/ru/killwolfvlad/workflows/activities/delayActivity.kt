package ru.killwolfvlad.workflows.activities

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

suspend fun delayActivity(
    activityContext: ActivityContext,
    activityId: String,
    duration: Duration,
) = withActivity(activityContext, activityId) {
    var untilDate = activityContext.getUntilDate()

    if (untilDate == null) {
        untilDate = Clock.System.now() + duration

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
