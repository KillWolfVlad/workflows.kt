package ru.killwolfvlad.workflows.core.annotations

/**
 * This API may slow down your application performance, please use it with caution!
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API may slow down your application performance, please use it with caution!",
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS,
)
annotation class WorkflowsPerformance
