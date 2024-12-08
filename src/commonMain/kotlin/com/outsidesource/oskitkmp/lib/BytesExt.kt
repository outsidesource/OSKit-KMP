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

fun ByteArray.toShort(): Short =
    (
        (this[2].toInt() and 0xFF shl 8) or
            (this[3].toInt() and 0xFF)
        ).toShort()

fun Short.toByteArray(): ByteArray = byteArrayOf(
    ((this.toInt() shr 8) and 0xFF).toByte(),
    (this.toInt() and 0xFF).toByte(),
)

fun ByteArray.toInt(): Int =
    (this[0].toInt() and 0xFF shl 24) or
        (this[1].toInt() and 0xFF shl 16) or
        (this[2].toInt() and 0xFF shl 8) or
        (this[3].toInt() and 0xFF)

fun Int.toByteArray(): ByteArray = byteArrayOf(
    ((this.toInt() shr 24) and 0xFF).toByte(),
    ((this.toInt() shr 16) and 0xFF).toByte(),
    ((this.toInt() shr 8) and 0xFF).toByte(),
    (this.toInt() and 0xFF).toByte(),
)

fun ByteArray.toLong(): Long =
    (this[0].toLong() and 0xFF shl 56) or
        (this[1].toLong() and 0xFF shl 48) or
        (this[2].toLong() and 0xFF shl 40) or
        (this[3].toLong() and 0xFF shl 32) or
        (this[4].toLong() and 0xFF shl 24) or
        (this[5].toLong() and 0xFF shl 16) or
        (this[6].toLong() and 0xFF shl 8) or
        (this[7].toLong() and 0xFF)

fun Long.toByteArray(): ByteArray = byteArrayOf(
    ((this.toInt() shr 56) and 0xFF).toByte(),
    ((this.toInt() shr 48) and 0xFF).toByte(),
    ((this.toInt() shr 40) and 0xFF).toByte(),
    ((this.toInt() shr 32) and 0xFF).toByte(),
    ((this.toInt() shr 24) and 0xFF).toByte(),
    ((this.toInt() shr 16) and 0xFF).toByte(),
    ((this.toInt() shr 8) and 0xFF).toByte(),
    (this.toInt() and 0xFF).toByte(),
)

fun ByteArray.toFloat(): Float = Float.fromBits(toInt())

fun Float.toByteArray(): ByteArray = toRawBits().toByteArray()

fun ByteArray.toDouble(): Double = Double.fromBits(toLong())

fun Double.toByteArray(): ByteArray = toRawBits().toByteArray()
