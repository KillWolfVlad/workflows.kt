package ru.killwolfvlad.workflows.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.serializer
import ru.killwolfvlad.workflows.core.annotations.WorkflowsPerformance
import ru.killwolfvlad.workflows.core.coroutines.getWorkflowContext
import ru.killwolfvlad.workflows.core.types.WithWorkflowContextCallback
import kotlin.coroutines.coroutineContext

@Suppress("UNCHECKED_CAST")
@OptIn(WorkflowsPerformance::class, ExperimentalSerializationApi::class)
suspend inline fun <reified TWorkflowContext> withWorkflowContext(block: WithWorkflowContextCallback<TWorkflowContext>) {
    val workflowContextKeys = serializer<TWorkflowContext>().descriptor.elementNames.toList()
    val workflowContextMap = coroutineContext.getWorkflowContext().get(*workflowContextKeys.toTypedArray())
    val workflowContext = Properties.decodeFromStringMap<TWorkflowContext>(workflowContextMap as Map<String, String>)

    block(workflowContext)
}
