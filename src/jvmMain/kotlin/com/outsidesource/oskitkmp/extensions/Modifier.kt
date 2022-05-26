package com.outsidesource.oskitkmp.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.disablePointerInput(disabled: Boolean) = pointerInput(disabled) {
    if (!disabled) return@pointerInput

    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consumeAllChanges() }
        }
    }
}

fun Modifier.consumePointerInput(pass: PointerEventPass = PointerEventPass.Main) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(pass).changes.forEach { it.consumeAllChanges() }
        }
    }
}