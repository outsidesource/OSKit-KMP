package com.outsidesource.oskitkmp.tuples

sealed class Tuple

data class Tup1<out V1>(val v1: V1) : Tuple()
data class Tup2<out V1, out V2>(val v1: V1, val v2: V2) : Tuple()
data class Tup3<out V1, out V2, out V3>(val v1: V1, val v2: V2, val v3: V3) : Tuple()
data class Tup4<out V1, out V2, out V3, out V4>(val v1: V1, val v2: V2, val v3: V3, val v4: V4) : Tuple()
data class Tup5<out V1, out V2, out V3, out V4, out V5>(
    val v1: V1,
    val v2: V2,
    val v3: V3,
    val v4: V4,
    val v5: V5,
) : Tuple()
data class Tup6<out V1, out V2, out V3, out V4, out V5, out V6>(
    val v1: V1,
    val v2: V2,
    val v3: V3,
    val v4: V4,
    val v5: V5,
    val v6: V6,
) : Tuple()
data class Tup7<out V1, out V2, out V3, out V4, out V5, out V6, out V7>(
    val v1: V1,
    val v2: V2,
    val v3: V3,
    val v4: V4,
    val v5: V5,
    val v6: V6,
    val v7: V7,
) : Tuple()
