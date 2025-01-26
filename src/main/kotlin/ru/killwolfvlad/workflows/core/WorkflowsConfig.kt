package ru.killwolfvlad.workflows.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Duration

data class WorkflowsConfig(
    val workerId: String,
    val heartbeatInterval: Duration,
    val lockTimeout: Duration,
    val fetchInterval: Duration,
    val rootJob: Job = SupervisorJob(),
)
