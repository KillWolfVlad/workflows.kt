package ru.killwolfvlad.workflows.core.interfaces

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec

class WorkflowsExceptionHandlerTest : WorkflowsDescribeSpec({
    val workflowsExceptionHandler = mockk<WorkflowsExceptionHandler>()

    beforeEach {
        coJustRun {
            workflowsExceptionHandler.handle(any())
        }
    }

    describe("when no exception is thrown") {
        var result: Result<Long?>? = null

        beforeEach {
            result = workflowsExceptionHandler.runSafe { 10L }
        }

        it("must doesn't call handle") {
            coVerify(exactly = 0) { workflowsExceptionHandler.handle(any()) }
        }

        it("must return success") {
            result shouldBe Result.success(10L)
        }
    }

    describe("when cancellation exception is thrown") {
        var result: Result<Long?>? = null

        beforeEach {
            result =
                workflowsExceptionHandler.runSafe {
                    throw CancellationException()
                }
        }

        it("must doesn't call handle") {
            coVerify(exactly = 0) { workflowsExceptionHandler.handle(any()) }
        }

        it("must return null") {
            result shouldBe Result.success(null)
        }
    }

    describe("when exception is thrown") {
        var result: Result<Long?>? = null

        val exception = IllegalArgumentException("some message")

        beforeEach {
            result =
                workflowsExceptionHandler.runSafe {
                    throw exception
                }
        }

        it("must handle exception") {
            coVerify(exactly = 1) { workflowsExceptionHandler.handle(exception) }
        }

        it("must return failure") {
            result shouldBe Result.failure(exception)
        }
    }

    describe("when exception handler throw exception") {
        var result: Result<Long?>? = null

        val exception = IllegalArgumentException("some message")

        beforeEach {
            coEvery {
                workflowsExceptionHandler.handle(any())
            } throws IllegalArgumentException("some message from handler")

            result =
                workflowsExceptionHandler.runSafe {
                    throw exception
                }
        }

        it("must handle exception") {
            coVerify(exactly = 1) { workflowsExceptionHandler.handle(exception) }
        }

        it("must return failure") {
            result shouldBe Result.failure(exception)
        }
    }
})
