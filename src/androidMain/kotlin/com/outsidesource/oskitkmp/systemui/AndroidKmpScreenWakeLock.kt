package com.outsidesource.oskitkmp.systemui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager

class AndroidKmpScreenWakeLock(
    private val context: Context,
) : IKmpScreenWakeLock {

    private var window: Window? = null

    override suspend fun acquire() {
        val window = context.findActivity()?.window ?: return
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override suspend fun release() {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window = null
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
