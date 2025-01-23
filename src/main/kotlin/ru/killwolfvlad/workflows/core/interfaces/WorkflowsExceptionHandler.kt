package ru.killwolfvlad.workflows.core.interfaces

interface WorkflowsExceptionHandler {
    suspend fun handle(exception: Exception)
}
