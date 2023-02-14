package com.outsidesource.oskitkmp.lib

import androidx.compose.runtime.Composable

@Composable
expect fun KMPBackHandler(enabled: Boolean, onBack: () -> Unit)
