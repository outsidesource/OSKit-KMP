package com.outsidesource.oskitkmp.deeplink

actual sealed class KMPDeepLink {
    sealed class Desktop(val data: Any) : KMPDeepLink()
}
