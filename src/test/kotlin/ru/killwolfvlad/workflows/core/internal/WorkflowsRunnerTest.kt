package ru.killwolfvlad.workflows.core.internal

import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import ru.killwolfvlad.workflows.core.ActivityContext
import ru.killwolfvlad.workflows.core.WorkflowContext
import ru.killwolfvlad.workflows.core.interfaces.KeyValueClient
import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsClassManager
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsExceptionHandler
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_CLASS_NAME_FIELD_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_LOCKS_KEY
import ru.killwolfvlad.workflows.core.internal.consts.WORKFLOW_WORKERS_KEY
import ru.killwolfvlad.workflows.core.internal.enums.WorkflowSignal
import ru.killwolfvlad.workflows.core.internal.extensions.workflowClassName
import ru.killwolfvlad.workflows.core.types.WorkflowId
import ru.killwolfvlad.workflows.core.types.workflowKey
import ru.killwolfvlad.workflows.test.WorkflowsDescribeSpec
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private class TestWorkflow(
    private val doneCallback: () -> Unit,
) : Workflow {
    override suspend fun execute() {
        doneCallback()
    }
}

private class TestWorkflowWithDelay(
    private val doneCallback: () -> Unit,
) : Workflow {
    override suspend fun execute() {
        delay(10.minutes)
        doneCallback()
    }
}

class WorkflowsRunnerTest : WorkflowsDescribeSpec({
    val workflowId = WorkflowId("workflow1")

    val activityContext = mockk<ActivityContext>()
    val keyValueClient = mockk<KeyValueClient>()
    val workflowContext = mockk<WorkflowContext>()
    val workflowsClassManager = mockk<WorkflowsClassManager>()
    val workflowsExceptionHandler = mockk<WorkflowsExceptionHandler>()

    val testWorkflowDoneCallback = mockk<() -> Unit>()
    val testWorkflowWithDelayDoneCallback = mockk<() -> Unit>()

    val workflowsRunner =
        WorkflowsRunner(
            rootJob,
            activityContext,
            defaultWorkflowsConfig,
            keyValueClient,
            workflowContext,
            workflowsClassManager,
            workflowsExceptionHandler,
        )

    beforeEach {
        coEvery { workflowsClassManager.getInstance(any()) } coAnswers {
            val workflowClass = firstArg<KClass<out Workflow>>()

            when (workflowClass) {
                TestWorkflow::class -> TestWorkflow(testWorkflowDoneCallback)
                TestWorkflowWithDelay::class -> TestWorkflowWithDelay(testWorkflowWithDelayDoneCallback)
                else -> throw IllegalArgumentException("unknown workflow class: $workflowClass!")
            }
        }

        coJustRun {
            workflowsExceptionHandler.handle(any())
        }

        coJustRun {
            testWorkflowDoneCallback()
        }

        coJustRun {
            testWorkflowWithDelayDoneCallback()
        }
    }

    describe("when runner doesn't contains workflow") {
        beforeEach {
            coEvery {
                keyValueClient.eval<Long>("acquireWorkflowLock", any(), any(), *anyVararg())
            } returns 1L

            coJustRun {
                keyValueClient.eval<Unit>("deleteWorkflow", any(), any(), *anyVararg())
            }

            coEvery {
                keyValueClient.hGet(workflowId.workflowKey, WorkflowSignal.FIELD_KEY)
            } returns null

            workflowsRunner.run(workflowId, mapOf("field1" to "value2", "field2" to "value2"), TestWorkflow::class)
        }

        it("must execute workflow") {
            eventually(2.seconds) {
                coVerify(exactly = 1) { testWorkflowDoneCallback() }
            }
        }

        it("must remove workflow after execution") {
            eventually(2.seconds) {
                workflowsRunner.contains(workflowId) shouldBe false
            }
        }

        it("must don't throw any exceptions") {
            continually(2.seconds) {
                coVerify(exactly = 0) { workflowsExceptionHandler.handle(any()) }
            }
        }

        it("must acquire workflow lock") {
            eventually(2.seconds) {
                coVerify(exactly = 1) {
                    keyValueClient.eval<Long>(
                        "acquireWorkflowLock",
                        LuaScripts.acquireWorkflowLock,
                        listOf(
                            workflowId.workflowKey,
                            WORKFLOW_LOCKS_KEY,
                            WORKFLOW_WORKERS_KEY,
                        ),
                        workflowId.value,
                        defaultWorkflowsConfig.workerId,
                        "6",
                        WORKFLOW_CLASS_NAME_FIELD_KEY,
                        TestWorkflow::class.workflowClassName,
                        "ctx:field1",
                        "value2",
                        "ctx:field2",
                        "value2",
                    )
                }
            }
        }

        it("must delete workflow") {
            eventually(2.seconds) {
                coVerify(exactly = 1) {
                    keyValueClient.eval<Long>(
                        "deleteWorkflow",
                        LuaScripts.deleteWorkflow,
                        listOf(
                            workflowId.workflowKey,
                            WORKFLOW_LOCKS_KEY,
                        ),
                        workflowId.value,
                    )
                }
            }
        }
    }

    describe("when runner contains workflow") {
        beforeEach {
            coEvery {
                keyValueClient.eval<Long>("acquireWorkflowLock", any(), any(), *anyVararg())
            } returns 1L

            coJustRun {
                keyValueClient.eval<Unit>("deleteWorkflow", any(), any(), *anyVararg())
            }

            coEvery {
                keyValueClient.hGet(workflowId.workflowKey, WorkflowSignal.FIELD_KEY)
            } returns null

            workflowsRunner.run(workflowId, emptyMap(), TestWorkflowWithDelay::class)
            workflowsRunner.run(workflowId, emptyMap(), TestWorkflow::class)
        }

        it("must don't execute second workflow") {
            continually(2.seconds) {
                coVerify(exactly = 0) { testWorkflowDoneCallback() }
            }
        }

        it("must don't throw any exceptions") {
            continually(2.seconds) {
                coVerify(exactly = 0) { workflowsExceptionHandler.handle(any()) }
            }
        }
    }

    describe("when workflow lock doesn't acquired") {
        beforeEach {
            coEvery {
                keyValueClient.eval<Long>("acquireWorkflowLock", any(), any(), *anyVararg())
            } returns 0L

            workflowsRunner.run(workflowId, emptyMap(), TestWorkflow::class)
        }

        it("must don't execute workflow") {
            continually(2.seconds) {
                coVerify(exactly = 0) { testWorkflowDoneCallback() }
            }
        }

        it("must don't throw any exceptions") {
            continually(2.seconds) {
                coVerify(exactly = 0) { workflowsExceptionHandler.handle(any()) }
            }
        }
    }

    describe("when workflow is CANCELLED") {
        beforeEach {
            coEvery {
                keyValueClient.eval<Long>("acquireWorkflowLock", any(), any(), *anyVararg())
            } returns 1L

            coJustRun {
                keyValueClient.eval<Unit>("deleteWorkflow", any(), any(), *anyVararg())
            }

            coEvery {
                keyValueClient.hGet(workflowId.workflowKey, WorkflowSignal.FIELD_KEY)
            } returns WorkflowSignal.CANCEL.toString()

            workflowsRunner.run(workflowId, emptyMap(), TestWorkflow::class)
        }

        it("must don't execute workflow") {
            continually(2.seconds) {
                coVerify(exactly = 0) { testWorkflowDoneCallback() }
            }
        }

        it("must remove workflow after execution") {
            eventually(2.seconds) {
                workflowsRunner.contains(workflowId) shouldBe false
            }
        }

        it("must don't throw any exceptions") {
            continually(2.seconds) {
                coVerify(exactly = 0) { workflowsExceptionHandler.handle(any()) }
            }
        }

        it("must delete workflow") {
            eventually(2.seconds) {
                coVerify(exactly = 1) {
                    keyValueClient.eval<Long>(
                        "deleteWorkflow",
                        any(),
                        any(),
                        *anyVararg(),
                    )
                }
            }
        }
    }
})
