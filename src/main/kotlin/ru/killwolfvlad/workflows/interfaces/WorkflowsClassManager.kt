package ru.killwolfvlad.workflows.interfaces

import kotlin.reflect.KClass

interface WorkflowsClassManager {
    fun getInstance(workflowClass: KClass<out Workflow>): Workflow
}
