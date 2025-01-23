package com.outsidesource.oskitkmp.filesystem

import android.content.Context
import androidx.activity.ComponentActivity

actual data class KmpFsContext(
    val applicationContext: Context,
    val activity: ComponentActivity,
)
