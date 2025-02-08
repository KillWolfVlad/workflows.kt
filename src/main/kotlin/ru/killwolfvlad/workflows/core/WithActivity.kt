package ru.killwolfvlad.workflows.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import kotlinx.serialization.properties.encodeToStringMap
import kotlinx.serialization.serializer
import ru.killwolfvlad.workflows.core.types.SerializationActivityCallback
import ru.killwolfvlad.workflows.core.types.SerializationSimpleActivityCallback
import ru.killwolfvlad.workflows.core.types.SimpleActivityCallback

suspend inline fun withActivity(
    activityId: String,
    workflowContextKeys: List<String> = emptyList(),
    crossinline block: SimpleActivityCallback,
) = withActivity(activityId, workflowContextKeys = workflowContextKeys) { workflowContextMap, _ ->
    block(workflowContextMap)
}

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified TWorkflowContext, reified TActivityContext, reified TReturnedContext> withActivity(
    activityId: String,
    crossinline block: SerializationActivityCallback<TWorkflowContext, TActivityContext, TReturnedContext>,
) = withActivity(
    activityId,
    workflowContextKeys = serializer<TWorkflowContext>().descriptor.elementNames.toList(),
    activityContextKeys = serializer<TActivityContext>().descriptor.elementNames.toList(),
) { workflowContextMap, activityContextMap ->
    val workflowContext = Properties.decodeFromStringMap<TWorkflowContext>(workflowContextMap as Map<String, String>)
    val activityContext = Properties.decodeFromStringMap<TActivityContext>(activityContextMap as Map<String, String>)

    val returnedContext = block(workflowContext, activityContext)

    if (returnedContext != null) Properties.encodeToStringMap(returnedContext) else null
}

suspend inline fun <reified TWorkflowContext, reified TReturnedContext> withActivity(
    activityId: String,
    crossinline block: SerializationSimpleActivityCallback<TWorkflowContext, TReturnedContext>,
) = withActivity<TWorkflowContext, Unit, TReturnedContext>(activityId) { workflowContext, _ ->
    block(workflowContext)
}
