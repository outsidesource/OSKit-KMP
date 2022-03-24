object Versions {
    const val Kotlin = "1.6.10"
    const val ComposePlugin = "1.0.1"
    const val KotlinCompilerExtension = "1.1.0-rc2"
    const val KtLintPlugin = "10.2.1"

    const val CoroutinesCore = "1.6.0"
    const val AndroidXCore = "1.6.0"
    const val AndroidXActivityCompose = "1.3.1"
    const val AndroidXLifecycleViewModelCompose = "1.0.0-alpha07"
    const val KotlinxDateTime = "0.3.0"
    const val KotlinxAtomicFu = "0.17.0"
}

object Dependencies {
    const val CoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.CoroutinesCore}"
    const val AndroidXCore = "androidx.core:core-ktx:${Versions.AndroidXCore}"
    const val AndroidXActivityCompose = "androidx.activity:activity-compose:${Versions.AndroidXActivityCompose}"
    const val AndroidXLifecycleViewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.AndroidXLifecycleViewModelCompose}"
    const val KotlinxDateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${Versions.KotlinxDateTime}"
    const val KotlinxAtomicFu = "org.jetbrains.kotlinx:atomicfu:${Versions.KotlinxAtomicFu}"
}