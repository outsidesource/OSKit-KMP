package com.outsidesource.oskitkmp.io

sealed class KmpIoError(override val message: String) : Throwable(message) {
    data object EofError : KmpIoError("End of File")
    data class Unknown(val error: Any) : KmpIoError(error.toString())
}
