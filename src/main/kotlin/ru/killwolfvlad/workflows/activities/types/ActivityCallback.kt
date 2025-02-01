package ru.killwolfvlad.workflows.activities.types

typealias ActivityCallback = suspend (workflowContextMap: Map<String, String?>) -> Map<String, String>?
