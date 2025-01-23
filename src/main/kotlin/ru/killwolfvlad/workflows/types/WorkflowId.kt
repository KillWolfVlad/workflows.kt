package ru.killwolfvlad.workflows.types

import kotlinx.serialization.Serializable
import ru.killwolfvlad.workflows.consts.WORKFLOW_KEY_PREFIX

@JvmInline
@Serializable
value class WorkflowId(val value: String)

internal inline val WorkflowId.workflowKey: String
    get() = "${WORKFLOW_KEY_PREFIX}:${value}"
