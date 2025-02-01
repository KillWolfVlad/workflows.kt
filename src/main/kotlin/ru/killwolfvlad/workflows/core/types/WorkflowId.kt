package ru.killwolfvlad.workflows.core.types

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class WorkflowId(
    val value: String,
)

internal inline val WorkflowId.workflowKey: String
    get() = "workflows:$value"
