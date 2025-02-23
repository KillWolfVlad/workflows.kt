package ru.killwolfvlad.workflows.core.types

typealias SerializationActivityCallback<TWorkflowContext, TActivityContext, TReturnedContext> =
    suspend (workflowContext: TWorkflowContext, activityContext: TActivityContext) -> TReturnedContext
