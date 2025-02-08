package ru.killwolfvlad.workflows.core.internal.dtos

import kotlinx.serialization.Serializable
import ru.killwolfvlad.workflows.core.internal.enums.WorkflowSignal
import ru.killwolfvlad.workflows.core.types.WorkflowId

@Serializable
internal data class WorkflowSignalMessageDto(
    val workflowId: WorkflowId,
    val signal: WorkflowSignal,
)
