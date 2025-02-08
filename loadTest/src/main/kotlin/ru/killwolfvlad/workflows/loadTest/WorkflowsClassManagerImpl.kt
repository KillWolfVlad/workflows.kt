package ru.killwolfvlad.workflows.loadTest

import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsClassManager
import kotlin.reflect.KClass

class WorkflowsClassManagerImpl(
    private val loadTestWorkflow: LoadTestWorkflow,
) : WorkflowsClassManager {
    override fun getInstance(workflowClass: KClass<out Workflow>): Workflow =
        when (workflowClass) {
            LoadTestWorkflow::class -> loadTestWorkflow
            else -> throw IllegalArgumentException("unknown workflow class: $workflowClass!")
        }
}
