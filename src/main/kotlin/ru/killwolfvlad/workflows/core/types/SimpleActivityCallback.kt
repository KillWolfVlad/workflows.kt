package ru.killwolfvlad.workflows.core.types

typealias SimpleActivityCallback =
    suspend (workflowContextMap: Map<String, String?>) -> Map<String, String>?
