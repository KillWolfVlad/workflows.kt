package ru.killwolfvlad.workflows.core.interfaces

import kotlinx.coroutines.CancellationException
import kotlin.runCatching

interface WorkflowsExceptionHandler {
    suspend fun handle(exception: Exception)
}

internal suspend inline fun <T> WorkflowsExceptionHandler.runSafe(block: () -> T): Result<T?> =
    try {
        Result.success(block())
    } catch (_: CancellationException) {
        // skip cancellation exception
        Result.success(null)
    } catch (exception: Exception) {
        runCatching {
            handle(exception)
        }

        Result.failure(exception)
    }
