package ru.killwolfvlad.workflows.dtos

import kotlinx.serialization.Serializable
import ru.killwolfvlad.workflows.types.WorkflowId

@Serializable
internal data class WorkflowSignalMessageDto(
    val workflowId: WorkflowId,
    val signal: String,
)
