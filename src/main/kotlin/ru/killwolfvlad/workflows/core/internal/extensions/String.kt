package ru.killwolfvlad.workflows.core.internal.extensions

import ru.killwolfvlad.workflows.core.interfaces.Workflow
import kotlin.reflect.KClass

internal inline val String.workflowClass: KClass<out Workflow>
    get() = Class.forName(this).kotlin as KClass<out Workflow>
