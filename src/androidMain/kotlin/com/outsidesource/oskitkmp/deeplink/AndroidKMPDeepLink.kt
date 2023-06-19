package com.outsidesource.oskitkmp.deeplink

import android.content.Intent

actual sealed class KMPDeepLink {
    data class Android(val intent: Intent) : KMPDeepLink()
}
