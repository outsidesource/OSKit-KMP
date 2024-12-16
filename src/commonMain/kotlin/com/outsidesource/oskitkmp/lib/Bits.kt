package com.outsidesource.oskitkmp.lib

fun getBit(number: Int, position: Int): Boolean {
    return (number shr position) and 1 == 1
}

fun setBit(number: Int, position: Int): Int {
    return number or (1 shl position)
}

fun unsetBit(number: Int, position: Int): Int {
    return number and (1 shl position).inv()
}

fun getBits(number: Int, from: Int, to: Int): Int {
    val mask = (1 shl (to - from + 1)) - 1
    return (number shr from) and mask
}

fun setBits(number: Int, from: Int, to: Int, value: Int): Int {
    val mask = ((1 shl (to - from + 1)) - 1) shl from
    return (number and mask.inv()) or ((value shl from) and mask)
}

fun unsetBits(number: Int, from: Int, to: Int): Int {
    val mask = ((1 shl (to - from + 1)) - 1) shl from
    return number and mask.inv()
}

fun flipBit(number: Int, position: Int): Int {
    return number xor (1 shl position)
}

fun flipBits(number: Int, from: Int, to: Int): Int {
    val mask = ((1 shl (to - from + 1)) - 1) shl from
    return number xor mask
}
