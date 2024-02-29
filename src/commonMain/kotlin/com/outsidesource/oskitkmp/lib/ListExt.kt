package com.outsidesource.oskitkmp.lib

inline fun <reified R> List<Any>.findInstance(): R? {
    return find { it is R } as R?
}

//inline fun <T, reified R> List<T>.findMapped(mapper: (T) -> R?): R? {
//    return find { it is R } as R?
//}
//
//inline fun <T, R> List<T>.lastOrNullMapped(mapper: (T) -> R?): R? {
//    var value: R? = null
//    lastOrNull {
//        value = mapper(it) ?: return@lastOrNull false
//        true
//    }
//    return value
//}
//
//inline fun <T, R> List<T>.firstOrNullMapped(mapper: (T) -> R?): R? {
//    var value: R? = null
//    lastOrNull {
//        value = mapper(it) ?: return@lastOrNull false
//        true
//    }
//    return value
//}