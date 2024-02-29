package com.outsidesource.oskitkmp.lib

inline fun <reified R> List<Any>.findInstance(predicate: (R) -> Boolean = { true }): R? {
    return find { it is R && predicate(it) } as R?
}

inline fun <T, reified R> List<T>.findMapped(transform: (T) -> R?): R? {
    for (item in this) {
        return transform(item) ?: continue
    }
    return null
}

inline fun <T, R> List<T>.lastNotNullOfOrNull(transform: (T) -> R?): R? {
    val iterator = listIterator(size)
    while (iterator.hasPrevious()) {
        val element = iterator.previous()
        return transform(element) ?: continue
    }
    return null
}