package com.outsidesource.oskitkmp.lib

object Bits {

    inline fun <reified T : Number> getBit(number: T, position: Int): Boolean {
        val num = number.toLong()
        return (num shr position) and 0x01L == 0x01L
    }

    inline fun <reified T : Number> setBit(number: T, position: Int): T {
        val num = number.toLong()
        return (num or (0x01L shl position)).toTyped()
    }

    inline fun <reified T : Number> unsetBit(number: T, position: Int): T {
        val num = number.toLong()
        return (num and (0x01L shl position).inv()).toTyped()
    }

    inline fun <reified T : Number> getBits(number: T, from: Int, to: Int): Long {
        val num = number.toLong()
        val mask = (0x01L shl (to - from + 1)) - 1
        return (num shr from) and mask
    }

    inline fun <reified T : Number> setBits(number: T, from: Int, to: Int, value: Long): T {
        val num = number.toLong()
        val mask = ((0x01L shl (to - from + 1)) - 1) shl from
        return ((num and mask.inv()) or ((value shl from) and mask)).toTyped()
    }

    inline fun <reified T : Number> unsetBits(number: T, from: Int, to: Int): T {
        val num = number.toLong()
        val mask = ((0x01L shl (to - from + 1)) - 1) shl from
        return (num and mask.inv()).toTyped()
    }

    inline fun <reified T : Number> flipBit(number: T, position: Int): T {
        val num = number.toLong()
        return (num xor (0x01L shl position)).toTyped()
    }

    inline fun <reified T : Number> flipBits(number: T, from: Int, to: Int): T {
        val num = number.toLong()
        val mask = ((0x01L shl (to - from + 1)) - 1) shl from
        return (num xor mask).toTyped()
    }

    inline fun <reified T : Number> Long.toTyped(): T = when (T::class) {
        Int::class -> this.toInt() as T
        Short::class -> this.toShort() as T
        Long::class -> this as T
        else -> throw IllegalArgumentException("Unsupported type")
    }
}
