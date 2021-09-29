import java.io.File
import java.io.FileInputStream
import java.util.*

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${Versions.AtomicFu}")
    }
}

plugins {
    kotlin("multiplatform") version Versions.Kotlin
    id("org.jetbrains.compose") version Versions.ComposePlugin
    id("com.android.library")
    id("maven-publish")
}
apply(plugin = "kotlinx-atomicfu")
apply(from = "versioning.gradle.kts")

val versionProperty = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, "version.properties")))
}[""] ?: "0.1.0"

group = "com.outsidesource"
version = versionProperty

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://plugins.gradle.org/m2/")
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    android()
    ios {
        binaries {
            framework {
                baseName = "oskit-kmp"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Dependencies.KotlinxDateTime)
                implementation(Dependencies.CoroutinesCore) {
                    version {
                        strictly(Versions.CoroutinesCore)
                    }
                }
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(Dependencies.AndroidXCore)
                implementation(Dependencies.AndroidXActivityCompose)
                implementation(Dependencies.AndroidXLifecycleViewModelCompose)
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
            }
        }
        val desktopTest by getting
        val iosMain by getting
        val iosTest by getting
    }

    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("maven") {
                    groupId = "com.outsidesource.oskitkmp"
                    artifactId = "oskitkmp"
                    version = versionProperty as String
                    artifact("$buildDir/outputs/aar/OSKit-kmp-release.aar")
                }
            }

            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/outsidesource/OSKit-KMP")
                    credentials {
                        username = System.getenv("OSD_DEVELOPER")
                        password = System.getenv("OSD_TOKEN")
                    }
                }
            }
        }
    }
}

android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(30)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.KotlinCompilerExtension
        kotlinCompilerVersion = Versions.Kotlin
    }
}