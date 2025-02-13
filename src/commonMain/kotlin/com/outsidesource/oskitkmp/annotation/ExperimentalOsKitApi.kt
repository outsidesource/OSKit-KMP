package com.outsidesource.oskitkmp.annotation

/**
 * This annotation marks the library API that is considered experimental and is not subject to the
 * general compatibility guarantees. The behavior of such API may be changed or the API may be removed completely in
 * any further release.
 *
 * > Beware using the annotated API especially if you're developing a library, since your library might become binary
 * incompatible with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalOSKitAPI` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalOSKitAPI::class)`,
 * or by using the compiler argument `-opt-in=kotlin.ExperimentalOSKitAPI`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
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
@MustBeDocumented
public annotation class ExperimentalOsKitApi
