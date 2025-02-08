package ru.killwolfvlad.workflows.core.internal.extensions

import kotlin.reflect.KClass

internal inline val <T : Any> KClass<T>.workflowClassName: String
    get() = qualifiedName ?: throw NullPointerException("$this don't have qualifiedName!")
