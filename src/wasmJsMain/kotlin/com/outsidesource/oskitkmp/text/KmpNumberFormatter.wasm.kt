@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.text

actual class KmpNumberFormatter actual constructor(
    private val minimumFractionDigits: Int,
    private val maximumFractionDigits: Int,
    private val useGrouping: Boolean,
) {
    private val formatter =
        Intl.NumberFormat(
            // An empty list uses the browser's locale, like DecimalFormat on the JVM. Null would throw.
            locales = JsArray(),
            options =
                numberFormatOptions(
                    minimumFractionDigits = minimumFractionDigits,
                    maximumFractionDigits = maximumFractionDigits,
                    useGrouping = useGrouping,
                ),
        )

    actual fun format(value: Float): String = formatter.format(value)
    actual fun format(value: Double): String = formatter.format(value)
    actual fun format(value: Int): String = formatter.format(value)
    actual fun format(value: Long): String = formatter.format(value)

    actual companion object
}

private external object Intl {
    class NumberFormat(locales: JsArray<JsString>, options: JsAny) {
        fun format(value: Float): String
        fun format(value: Double): String
        fun format(value: Int): String
        fun format(value: Long): String
    }
}

private fun numberFormatOptions(
    minimumFractionDigits: Int,
    maximumFractionDigits: Int,
    useGrouping: Boolean,
): JsAny = js(
    """
    ({
        "minimumFractionDigits": minimumFractionDigits,
        "maximumFractionDigits": maximumFractionDigits,
        "useGrouping": useGrouping,
    })
    """
)
