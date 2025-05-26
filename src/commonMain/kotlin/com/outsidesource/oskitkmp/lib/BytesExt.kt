package com.outsidesource.oskitkmp.lib

fun String.decodeHex(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()

fun ByteArray.encodeToHex(): String = joinToString(separator = "") {
    it.toUByte().toString(16).padStart(2, '0')
}

fun ByteArray.find(
    pattern: ByteArray,
    fromIndex: Int = 0
): Int {
    if (pattern.isEmpty()) return -1
    val lastStart = size - pattern.size

    outer@ for (i in fromIndex.coerceAtLeast(0)..lastStart) {
        for (j in pattern.indices) {
            if (this[i + j] != pattern[j]) continue@outer
        }
        return i
    }
    return -1
}

fun UInt.reverse(): UInt {
    var reversed = 0u
    var input = this

    for (i in 0 until UInt.SIZE_BITS) {
        reversed = reversed shl 1
        if (input and 1u == 1u) reversed = reversed xor 1u
        input = input shr 1
    }

    return reversed
}

fun ByteArray.toShort(start: Int = 0): Short =
    (((this[start + 0].toInt() and 0xFF) shl 8) or (this[start + 1].toInt() and 0xFF)).toShort()

fun ByteArray.toShortLe(start: Int = 0): Short = (
    (this[start + 0].toInt() and 0xFF) or
        ((this[start + 1].toInt() and 0xFF) shl 8)
    ).toShort()

fun Short.toBytes(buffer: ByteArray = ByteArray(2), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this.toInt() shr 8) and 0xFF).toByte()
    buffer[start + 1] = (this.toInt() and 0xFF).toByte()
    return buffer
}

fun Short.toBytesLe(buffer: ByteArray = ByteArray(2), start: Int = 0): ByteArray {
    buffer[start + 0] = (this.toInt() and 0xFF).toByte()
    buffer[start + 1] = ((this.toInt() shr 8) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toUShort(start: Int = 0): UShort =
    (((this[start + 0].toInt() and 0xFF) shl 8) or (this[start + 1].toInt() and 0xFF)).toUShort()

fun ByteArray.toUShortLe(start: Int = 0): UShort =
    ((this[start + 0].toInt() and 0xFF) or ((this[start + 1].toInt() and 0xFF) shl 8)).toUShort()

fun UShort.toBytes(buffer: ByteArray = ByteArray(2), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this.toInt() shr 8) and 0xFF).toByte()
    buffer[start + 1] = (this.toInt() and 0xFF).toByte()
    return buffer
}

fun UShort.toBytesLe(buffer: ByteArray = ByteArray(2), start: Int = 0): ByteArray {
    buffer[start + 0] = (this.toInt() and 0xFF).toByte()
    buffer[start + 1] = ((this.toInt() shr 8) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toInt(start: Int = 0): Int =
    ((this[start + 0].toInt() and 0xFF) shl 24) or
        ((this[start + 1].toInt() and 0xFF) shl 16) or
        ((this[start + 2].toInt() and 0xFF) shl 8) or
        (this[start + 3].toInt() and 0xFF)

fun ByteArray.toIntLe(start: Int = 0): Int =
    (this[start + 0].toInt() and 0xFF) or
        ((this[start + 1].toInt() and 0xFF) shl 8) or
        ((this[start + 2].toInt() and 0xFF) shl 16) or
        ((this[start + 3].toInt() and 0xFF) shl 24)

fun Int.toBytes(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this shr 24) and 0xFF).toByte()
    buffer[start + 1] = ((this shr 16) and 0xFF).toByte()
    buffer[start + 2] = ((this shr 8) and 0xFF).toByte()
    buffer[start + 3] = (this and 0xFF).toByte()
    return buffer
}

fun Int.toBytesLe(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray {
    buffer[start + 0] = (this and 0xFF).toByte()
    buffer[start + 1] = ((this shr 8) and 0xFF).toByte()
    buffer[start + 2] = ((this shr 16) and 0xFF).toByte()
    buffer[start + 3] = ((this shr 24) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toUInt(start: Int = 0): UInt = (
    ((this[start + 0].toInt() and 0xFF) shl 24) or
        ((this[start + 1].toInt() and 0xFF) shl 16) or
        ((this[start + 2].toInt() and 0xFF) shl 8) or
        (this[start + 3].toInt() and 0xFF)
    ).toUInt()

fun ByteArray.toUIntLe(start: Int = 0): UInt = (
    (this[start + 0].toInt() and 0xFF) or
        ((this[start + 1].toInt() and 0xFF) shl 8) or
        ((this[start + 2].toInt() and 0xFF) shl 16) or
        ((this[start + 3].toInt() and 0xFF) shl 24)
    ).toUInt()

fun UInt.toBytes(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this shr 24) and 0xFFu).toByte()
    buffer[start + 1] = ((this shr 16) and 0xFFu).toByte()
    buffer[start + 2] = ((this shr 8) and 0xFFu).toByte()
    buffer[start + 3] = (this and 0xFFu).toByte()
    return buffer
}

fun UInt.toBytesLe(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray {
    buffer[start + 0] = (this and 0xFFu).toByte()
    buffer[start + 1] = ((this shr 8) and 0xFFu).toByte()
    buffer[start + 2] = ((this shr 16) and 0xFFu).toByte()
    buffer[start + 3] = ((this shr 24) and 0xFFu).toByte()
    return buffer
}

fun ByteArray.toLong(start: Int = 0): Long =
    ((this[start + 0].toLong() and 0xFF) shl 56) or
        ((this[start + 1].toLong() and 0xFF) shl 48) or
        ((this[start + 2].toLong() and 0xFF) shl 40) or
        ((this[start + 3].toLong() and 0xFF) shl 32) or
        ((this[start + 4].toLong() and 0xFF) shl 24) or
        ((this[start + 5].toLong() and 0xFF) shl 16) or
        ((this[start + 6].toLong() and 0xFF) shl 8) or
        (this[start + 7].toLong() and 0xFF)

fun ByteArray.toLongLe(start: Int = 0): Long =
    (this[start + 0].toLong() and 0xFF) or
        ((this[start + 1].toLong() and 0xFF) shl 8) or
        ((this[start + 2].toLong() and 0xFF) shl 16) or
        ((this[start + 3].toLong() and 0xFF) shl 24) or
        ((this[start + 4].toLong() and 0xFF) shl 32) or
        ((this[start + 5].toLong() and 0xFF) shl 40) or
        ((this[start + 6].toLong() and 0xFF) shl 48) or
        ((this[start + 7].toLong() and 0xFF) shl 56)

fun Long.toBytes(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this shr 56) and 0xFF).toByte()
    buffer[start + 1] = ((this shr 48) and 0xFF).toByte()
    buffer[start + 2] = ((this shr 40) and 0xFF).toByte()
    buffer[start + 3] = ((this shr 32) and 0xFF).toByte()
    buffer[start + 4] = ((this shr 24) and 0xFF).toByte()
    buffer[start + 5] = ((this shr 16) and 0xFF).toByte()
    buffer[start + 6] = ((this shr 8) and 0xFF).toByte()
    buffer[start + 7] = (this and 0xFF).toByte()
    return buffer
}

fun Long.toBytesLe(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray {
    buffer[start + 0] = (this and 0xFF).toByte()
    buffer[start + 1] = ((this shr 8) and 0xFF).toByte()
    buffer[start + 2] = ((this shr 16) and 0xFF).toByte()
    buffer[start + 3] = ((this shr 24) and 0xFF).toByte()
    buffer[start + 4] = ((this shr 32) and 0xFF).toByte()
    buffer[start + 5] = ((this shr 40) and 0xFF).toByte()
    buffer[start + 6] = ((this shr 48) and 0xFF).toByte()
    buffer[start + 7] = ((this shr 56) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toULong(start: Int = 0): ULong = (
    ((this[start + 0].toLong() and 0xFF) shl 56) or
        ((this[start + 1].toLong() and 0xFF) shl 48) or
        ((this[start + 2].toLong() and 0xFF) shl 40) or
        ((this[start + 3].toLong() and 0xFF) shl 32) or
        ((this[start + 4].toLong() and 0xFF) shl 24) or
        ((this[start + 5].toLong() and 0xFF) shl 16) or
        ((this[start + 6].toLong() and 0xFF) shl 8) or
        (this[start + 7].toLong() and 0xFF)
    ).toULong()

fun ByteArray.toULongLe(start: Int = 0): ULong = (
    (this[start + 0].toLong() and 0xFF) or
        ((this[start + 1].toLong() and 0xFF) shl 8) or
        ((this[start + 2].toLong() and 0xFF) shl 16) or
        ((this[start + 3].toLong() and 0xFF) shl 24) or
        ((this[start + 4].toLong() and 0xFF) shl 32) or
        ((this[start + 5].toLong() and 0xFF) shl 40) or
        ((this[start + 6].toLong() and 0xFF) shl 48) or
        ((this[start + 7].toLong() and 0xFF) shl 56)
    ).toULong()

fun ULong.toBytes(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this shr 56) and 0xFFu).toByte()
    buffer[start + 1] = ((this shr 48) and 0xFFu).toByte()
    buffer[start + 2] = ((this shr 40) and 0xFFu).toByte()
    buffer[start + 3] = ((this shr 32) and 0xFFu).toByte()
    buffer[start + 4] = ((this shr 24) and 0xFFu).toByte()
    buffer[start + 5] = ((this shr 16) and 0xFFu).toByte()
    buffer[start + 6] = ((this shr 8) and 0xFFu).toByte()
    buffer[start + 7] = (this and 0xFFu).toByte()
    return buffer
}

fun ULong.toBytesLe(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray {
    buffer[start + 0] = (this and 0xFFu).toByte()
    buffer[start + 1] = ((this shr 8) and 0xFFu).toByte()
    buffer[start + 2] = ((this shr 16) and 0xFFu).toByte()
    buffer[start + 3] = ((this shr 24) and 0xFFu).toByte()
    buffer[start + 4] = ((this shr 32) and 0xFFu).toByte()
    buffer[start + 5] = ((this shr 40) and 0xFFu).toByte()
    buffer[start + 6] = ((this shr 48) and 0xFFu).toByte()
    buffer[start + 7] = ((this shr 56) and 0xFFu).toByte()
    return buffer
}

fun ByteArray.toFloat(start: Int = 0): Float = Float.fromBits(toInt(start))

fun ByteArray.toFloatLe(start: Int = 0): Float = Float.fromBits(toIntLe(start))

fun Float.toBytes(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray =
    toRawBits().toBytes(buffer, start)

fun Float.toBytesLe(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray =
    toRawBits().toBytesLe(buffer, start)

fun ByteArray.toDouble(start: Int = 0): Double = Double.fromBits(toLong(start))

fun ByteArray.toDoubleLe(start: Int = 0): Double = Double.fromBits(toLongLe(start))

fun Double.toBytes(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray =
    toRawBits().toBytes(buffer, start)

fun Double.toBytesLe(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray =
    toRawBits().toBytesLe(buffer, start)
