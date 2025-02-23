package ru.killwolfvlad.workflows.core.types

typealias SerializationSimpleActivityCallback<TWorkflowContext, TReturnedContext> =
    suspend (workflowContext: TWorkflowContext) -> TReturnedContext
