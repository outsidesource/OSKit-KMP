object Versions {
    const val Kotlin = "1.9.20"
    const val KtLintPlugin = "11.6.1"

    const val CoroutinesCore = "1.7.1"
    const val KotlinxDateTime = "0.4.0"
    const val KotlinxAtomicFu = "0.23.2"
    const val KotlinxSerializationJson = "1.5.1"
    const val KotlinxSerializationCBOR = "1.5.1"
    const val Ktor = "2.3.1"
    const val OkIO = "3.5.0"
}

object Dependencies {
    const val CoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.CoroutinesCore}"
    const val KotlinxDateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${Versions.KotlinxDateTime}"
    const val KotlinxAtomicFu = "org.jetbrains.kotlinx:atomicfu:${Versions.KotlinxAtomicFu}"
    const val KotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KotlinxSerializationJson}"
    const val KotlinxSerializationCBOR = "org.jetbrains.kotlinx:kotlinx-serialization-cbor:${Versions.KotlinxSerializationCBOR}"
    const val KtorWebsockets = "io.ktor:ktor-server-websockets:${Versions.Ktor}"
    const val KtorServerCore = "io.ktor:ktor-server-core:${Versions.Ktor}"
    const val KtorServerCIO = "io.ktor:ktor-server-cio:${Versions.Ktor}"
    const val KtorClientCore = "io.ktor:ktor-client-core:${Versions.Ktor}"
    const val KtorClientCIO = "io.ktor:ktor-client-cio:${Versions.Ktor}"
    const val OkIO = "com.squareup.okio:okio:${Versions.OkIO}"
}