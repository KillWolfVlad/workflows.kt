package ru.killwolfvlad.workflows.core.internal.enums

internal enum class WorkflowSignal {
    CANCEL,
    ;

    companion object {
        const val CHANNEL = "workflows:signals"

        const val FIELD_KEY = "signal"
    }
}
