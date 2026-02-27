package com.outsidesource.oskitkmp.lib

fun <T> Iterable<T>.containsAny(list: List<T>) = any { it in list }
fun <T> Iterable<T>.containsAny(vararg list: T) = any { it in list }
fun <T> Collection<T>.containsAll(vararg list: T) = containsAll(list.toList())

inline fun <reified R> Iterable<*>.filterIsInstance(predicate: (R) -> Boolean): List<R> =
    filterIsInstanceTo(mutableListOf(), predicate)

inline fun <reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceTo(
    destination: C,
    predicate: (R) -> Boolean,
): C {
    for (element in this) if (element is R && predicate(element)) destination.add(element)
    return destination
}

fun <T> List<List<T>>.intersect(): List<T> {
    if (this.isEmpty()) return listOf()
    var res = this[0].toSet()
    forEach { res = res.intersect(it.toSet()) }
    return res.toList()
}

inline fun <reified R> Iterable<Any>.findInstance(predicate: (R) -> Boolean = { true }): R? {
    return find { it is R && predicate(it) } as R?
}

inline fun <T, reified R> Iterable<T>.findMapped(transform: (T) -> R?): R? {
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

inline fun <K, V, R> Map<K, V>.mapValuesNotNull(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
    val map = LinkedHashMap<K, R>(size)
    for (entry in this@mapValuesNotNull.entries) {
        map[entry.key] = transform(entry) ?: continue
    }
    return map
}

inline fun <K, V> Map<K, V>.update(block: MutableMap<K, V>.() -> Unit): Map<K, V> = toMutableMap().apply { block() }
