package ru.killwolfvlad.workflows

import kotlin.time.Duration

data class WorkflowsConfig(
    val workerId: String,
    val heartbeatInterval: Duration,
    val lockTimeout: Duration,
    val fetchInterval: Duration,
)
