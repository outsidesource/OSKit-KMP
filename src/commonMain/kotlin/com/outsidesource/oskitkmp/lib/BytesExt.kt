package com.outsidesource.oskitkmp.lib

fun String.decodeHex(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()

fun ByteArray.encodeToHex(): String = joinToString(separator = "") {
    it.toUByte().toString(16).padStart(2, '0')
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

fun Int.toBytesLe(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray {
    buffer[start] = (this and 0xFF).toByte()
    buffer[start + 1] = ((this shr 8) and 0xFF).toByte()
    buffer[start + 2] = ((this shr 16) and 0xFF).toByte()
    buffer[start + 3] = ((this shr 24) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toIntLe(start: Int = 0): Int {
    return (this[start].toInt() and 0xFF) or
        ((this[start + 1].toInt() and 0xFF) shl 8) or
        ((this[start + 2].toInt() and 0xFF) shl 16) or
        ((this[start + 3].toInt() and 0xFF) shl 24)
}

fun Short.toBytesLe(buffer: ByteArray = ByteArray(2), start: Int = 0): ByteArray {
    buffer[start] = (this.toInt() and 0xFF).toByte()
    buffer[start + 1] = ((this.toInt() shr 8) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toShortLe(start: Int = 0): Short {
    return (
        this[start].toInt() and 0xFF or
            ((this[start + 1].toInt() and 0xFF) shl 8)
        ).toShort()
}

fun Long.toBytesLe(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray {
    buffer[start] = (this and 0xFF).toByte()
    buffer[start + 1] = ((this shr 8) and 0xFF).toByte()
    buffer[start + 2] = ((this shr 16) and 0xFF).toByte()
    buffer[start + 3] = ((this shr 24) and 0xFF).toByte()
    buffer[start + 4] = ((this shr 32) and 0xFF).toByte()
    buffer[start + 5] = ((this shr 40) and 0xFF).toByte()
    buffer[start + 6] = ((this shr 48) and 0xFF).toByte()
    buffer[start + 7] = ((this shr 56) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toLongLe(start: Int = 0): Long {
    return (this[start].toLong() and 0xFF) or
        ((this[start + 1].toLong() and 0xFF) shl 8) or
        ((this[start + 2].toLong() and 0xFF) shl 16) or
        ((this[start + 3].toLong() and 0xFF) shl 24) or
        ((this[start + 4].toLong() and 0xFF) shl 32) or
        ((this[start + 5].toLong() and 0xFF) shl 40) or
        ((this[start + 6].toLong() and 0xFF) shl 48) or
        ((this[start + 7].toLong() and 0xFF) shl 56)
}

fun Float.toBytesLe(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray {
    val intBits = toRawBits()
    buffer[start] = (intBits and 0xFF).toByte()
    buffer[start + 1] = ((intBits shr 8) and 0xFF).toByte()
    buffer[start + 2] = ((intBits shr 16) and 0xFF).toByte()
    buffer[start + 3] = ((intBits shr 24) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toFloatLe(start: Int = 0): Float {
    val intBits = (this[start].toInt() and 0xFF) or
        ((this[start + 1].toInt() and 0xFF) shl 8) or
        ((this[start + 2].toInt() and 0xFF) shl 16) or
        ((this[start + 3].toInt() and 0xFF) shl 24)
    return Float.fromBits(intBits)
}

fun Double.toBytesLe(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray {
    val longBits = toRawBits()
    buffer[start] = (longBits and 0xFF).toByte()
    buffer[start + 1] = ((longBits shr 8) and 0xFF).toByte()
    buffer[start + 2] = ((longBits shr 16) and 0xFF).toByte()
    buffer[start + 3] = ((longBits shr 24) and 0xFF).toByte()
    buffer[start + 4] = ((longBits shr 32) and 0xFF).toByte()
    buffer[start + 5] = ((longBits shr 40) and 0xFF).toByte()
    buffer[start + 6] = ((longBits shr 48) and 0xFF).toByte()
    buffer[start + 7] = ((longBits shr 56) and 0xFF).toByte()
    return buffer
}

fun ByteArray.toDoubleLe(start: Int = 0): Double {
    val longBits = (this[start].toLong() and 0xFFL) or
        ((this[start + 1].toLong() and 0xFFL) shl 8) or
        ((this[start + 2].toLong() and 0xFFL) shl 16) or
        ((this[start + 3].toLong() and 0xFFL) shl 24) or
        ((this[start + 4].toLong() and 0xFFL) shl 32) or
        ((this[start + 5].toLong() and 0xFFL) shl 40) or
        ((this[start + 6].toLong() and 0xFFL) shl 48) or
        ((this[start + 7].toLong() and 0xFFL) shl 56)
    return Double.fromBits(longBits)
}
