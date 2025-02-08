package ru.killwolfvlad.workflows.exampleApp

import ru.killwolfvlad.workflows.core.interfaces.Workflow
import ru.killwolfvlad.workflows.core.interfaces.WorkflowsClassManager
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class WorkflowsClassManagerImpl : WorkflowsClassManager {
    override fun getInstance(workflowClass: KClass<out Workflow>): Workflow = workflowClass.primaryConstructor!!.call()
}
