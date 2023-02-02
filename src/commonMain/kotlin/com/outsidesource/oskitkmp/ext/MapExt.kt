package com.outsidesource.oskitkmp.ext

inline fun <K, V> MutableMap<K, V>.putIfAbsent(key: K, value: V): V {
    val v = get(key)
    if (v == null) {
        put(key, value)
        return value
    }
    return v
}
