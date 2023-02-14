object Versions {
    const val Kotlin = "1.8.0"
    const val ComposePlugin = "1.3.0"
    const val KotlinCompilerExtension = "1.4.0"
    const val KtLintPlugin = "10.2.1"

    const val CoroutinesCore = "1.6.4"
    const val AndroidXCore = "1.9.0"
    const val AndroidXActivityCompose = "1.6.1"
    const val AndroidXLifecycleViewModelCompose = "2.5.1"
    const val KotlinxDateTime = "0.4.0"
    const val KotlinxAtomicFu = "0.19.0"
    const val KotlinxSerializationJson = "1.5.0-RC"
    const val Ktor = "2.2.2"
    const val AndroidXComposeAnimations = "1.3.3"
}

object Dependencies {
    const val CoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.CoroutinesCore}"
    const val AndroidXCore = "androidx.core:core-ktx:${Versions.AndroidXCore}"
    const val AndroidXActivityCompose = "androidx.activity:activity-compose:${Versions.AndroidXActivityCompose}"
    const val AndroidXLifecycleViewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.AndroidXLifecycleViewModelCompose}"
    const val AndroidXComposeAnimations = "androidx.compose.animation:animation:${Versions.AndroidXComposeAnimations}"
    const val KotlinxDateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${Versions.KotlinxDateTime}"
    const val KotlinxAtomicFu = "org.jetbrains.kotlinx:atomicfu:${Versions.KotlinxAtomicFu}"
    const val KotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KotlinxSerializationJson}"
    const val KtorNetwork = "io.ktor:ktor-network:${Versions.Ktor}"
    const val KtorWebsockets = "io.ktor:ktor-server-websockets:${Versions.Ktor}"
    const val KtorServerCore = "io.ktor:ktor-server-core:${Versions.Ktor}"
    const val KtorServerCIO = "io.ktor:ktor-server-cio:${Versions.Ktor}"
    const val KtorClientCore = "io.ktor:ktor-client-core:${Versions.Ktor}"
    const val KtorClientCIO = "io.ktor:ktor-client-cio:${Versions.Ktor}"
}