package com.outsidesource.oskitkmp.lib

import io.ktor.http.*

data class KmpUrl(
    val scheme: String,
    val username: String?,
    val password: String?,
    val host: String,
    val port: Int?,
    val path: String,
    val query: String,
    val fragment: String,
    val encodedUrl: String,
) {

    override fun toString(): String = encodedUrl

    val queryParameters: Map<String, String> by lazy {
        buildMap {
            query.split("&").forEach {
                val param = it.split("=")
                this[param[0]] = param[1].decodeURLQueryComponent()
            }
        }
    }

    companion object {
        fun decodeUrlPart(value: String) = value.decodeURLPart()
        fun decodeQueryParam(value: String) = value.decodeURLQueryComponent()

        fun fromStringOrNull(value: String): KmpUrl? = try {
            fromString(value)
        } catch (t: Throwable) {
            null
        }

        fun fromString(value: String): KmpUrl {
            val url = Url(value)
            return KmpUrl(
                scheme = url.protocol.name,
                username = url.user,
                password = url.password,
                host = url.host,
                port = if (url.specifiedPort != 0) url.specifiedPort else null,
                path = url.encodedPath,
                query = url.encodedQuery,
                fragment = url.encodedFragment,
                encodedUrl = url.toString(),
            )
        }
    }
}
