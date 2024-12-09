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

fun ByteArray.toShort(start: Int = 0): Short =
    ((this[start + 0].toInt() and 0xFF shl 8) or (this[start + 1].toInt() and 0xFF)).toShort()

fun Short.toBytes(buffer: ByteArray = ByteArray(2), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this.toInt() shr 8) and 0xFF).toByte()
    buffer[start + 1] = (this.toInt() and 0xFF).toByte()
    return buffer
}

fun ByteArray.toInt(start: Int = 0): Int =
    (this[start + 0].toInt() and 0xFF shl 24) or
        (this[start + 1].toInt() and 0xFF shl 16) or
        (this[start + 2].toInt() and 0xFF shl 8) or
        (this[start + 3].toInt() and 0xFF)

fun Int.toBytes(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this.toInt() shr 24) and 0xFF).toByte()
    buffer[start + 1] = ((this.toInt() shr 16) and 0xFF).toByte()
    buffer[start + 2] = ((this.toInt() shr 8) and 0xFF).toByte()
    buffer[start + 3] = (this.toInt() and 0xFF).toByte()
    return buffer
}

fun ByteArray.toLong(start: Int = 0): Long =
    (this[start + 0].toLong() and 0xFF shl 56) or
        (this[start + 1].toLong() and 0xFF shl 48) or
        (this[start + 2].toLong() and 0xFF shl 40) or
        (this[start + 3].toLong() and 0xFF shl 32) or
        (this[start + 4].toLong() and 0xFF shl 24) or
        (this[start + 5].toLong() and 0xFF shl 16) or
        (this[start + 6].toLong() and 0xFF shl 8) or
        (this[start + 7].toLong() and 0xFF)

fun Long.toBytes(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray {
    buffer[start + 0] = ((this.toInt() shr 56) and 0xFF).toByte()
    buffer[start + 1] = ((this.toInt() shr 48) and 0xFF).toByte()
    buffer[start + 2] = ((this.toInt() shr 40) and 0xFF).toByte()
    buffer[start + 3] = ((this.toInt() shr 32) and 0xFF).toByte()
    buffer[start + 4] = ((this.toInt() shr 24) and 0xFF).toByte()
    buffer[start + 5] = ((this.toInt() shr 16) and 0xFF).toByte()
    buffer[start + 6] = ((this.toInt() shr 8) and 0xFF).toByte()
    buffer[start + 7] = (this.toInt() and 0xFF).toByte()
    return buffer
}

fun ByteArray.toFloat(start: Int = 0): Float = Float.fromBits(toInt(start))

fun Float.toBytes(buffer: ByteArray = ByteArray(4), start: Int = 0): ByteArray =
    toRawBits().toBytes(buffer, start)

fun ByteArray.toDouble(start: Int = 0): Double = Double.fromBits(toLong(start))

fun Double.toBytes(buffer: ByteArray = ByteArray(8), start: Int = 0): ByteArray =
    toRawBits().toBytes(buffer, start)
