package ru.killwolfvlad.workflows.core.types

typealias WithWorkflowContextCallback<TWorkflowContext> =
    suspend (workflowContext: TWorkflowContext) -> Unit
