package ru.killwolfvlad.workflows.interfaces

interface WorkflowsExceptionHandler {
    suspend fun handle(exception: Exception)
}
