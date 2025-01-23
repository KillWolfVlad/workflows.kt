package ru.killwolfvlad.workflows.extensions

import kotlin.reflect.KClass

internal inline val <T : Any> KClass<T>.workflowClassName: String
    get() = qualifiedName ?: throw NullPointerException("$this don't have qualifiedName!")
