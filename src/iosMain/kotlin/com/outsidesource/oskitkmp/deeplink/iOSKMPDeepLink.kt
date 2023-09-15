package com.outsidesource.oskitkmp.deeplink

actual sealed class KMPDeepLink {
    sealed class iOS(val data: Any) : KMPDeepLink()
}
