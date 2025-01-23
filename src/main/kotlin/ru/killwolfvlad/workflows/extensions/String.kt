package ru.killwolfvlad.workflows.extensions

import ru.killwolfvlad.workflows.interfaces.Workflow
import kotlin.reflect.KClass

internal inline val String.workflowClass: KClass<out Workflow>
    get() = Class.forName(this).kotlin as KClass<out Workflow>
