package ru.killwolfvlad.workflows.core.internal.enums

import ru.killwolfvlad.workflows.core.coroutines.getActivityId
import ru.killwolfvlad.workflows.core.internal.consts.ACTIVITY_FIELD_KEY_PREFIX
import kotlin.coroutines.coroutineContext

internal enum class ActivityStatus {
    COMPLETED,
    ;

    companion object {
        suspend inline fun getFieldKey(): String = "${ACTIVITY_FIELD_KEY_PREFIX}:${coroutineContext.getActivityId()}:status"
    }
}
