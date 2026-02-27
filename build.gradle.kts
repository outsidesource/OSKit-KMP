@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import java.io.FileInputStream
import java.util.*

repositories {
    mavenLocal()
    google {
        mavenContent {
            includeGroupAndSubgroups("androidx")
            includeGroupAndSubgroups("com.android")
            includeGroupAndSubgroups("com.google")
        }
    }
    gradlePluginPortal()
    mavenCentral()
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.dokka)
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.28.0"
    // Disable SQLDelight Gradle plugin until WASM support is released (https://github.com/sqldelight/sqldelight/pull/5531)
    // Pulled generated database from previous build
//    id("app.cash.sqldelight") version "2.0.2"
}

// Disable SQLDelight Gradle plugin until WASM support is released (https://github.com/sqldelight/sqldelight/pull/5531)
// Pulled generated database from previous build
//sqldelight {
//    databases {
//        create("KmpKvStoreDatabase") {
//            packageName.set("com.outsidesource.oskitkmp.storage")
//        }
//    }
//}

apply(from = "versioning.gradle.kts")

val versionProperty = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, "version.properties")))
}["version"] ?: "0.0.0"

group = "com.outsidesource"
version = versionProperty

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }

    androidLibrary {
        namespace = "com.outsidesource.oskitkmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries {
            framework {
                baseName = "oskitkmp"
//                linkerOpts("-lsqlite3") // I might be able to include this and not have to have consumers include it
            }
            getTest("DEBUG").linkerOpts("-lsqlite3")
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            binaries.executable()
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.ktor.server.core)
                implementation(libs.okio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val nonJsMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.sqldelight.runtime)
            }
        }
        val androidMain by getting {
            dependsOn(nonJsMain)
            dependencies {
                implementation(libs.sqldelight.android.driver)
                implementation(libs.activity.compose)
                implementation(libs.lifecycle.runtime)
                implementation(libs.documentfile)
            }
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.core)
                implementation(libs.junit)
            }
        }
        val iosMain by getting {
            dependsOn(nonJsMain)
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }
        val jvmMain by getting {
            dependsOn(nonJsMain)
            dependencies {
                implementation(libs.sqldelight.jvm.driver)
                implementation(libs.jna)
                implementation(libs.jna.platform)
                implementation(libs.dbus.java.core)
                implementation(libs.dbus.java.transport.native.unixsocket)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    configure(
        platform = KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
            sourcesJar = true,
            androidVariantsToPublish = listOf("debug", "release"),
        )
    )

    pom {
        description.set("An opinionated architecture/library for Kotlin Multiplatform development")
        name.set(project.name)
        url.set("https://github.com/outsidesource/OSKit-KMP")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://spdx.org/licenses/MIT.html")
                distribution.set("https://spdx.org/licenses/MIT.html")
            }
        }
        scm {
            url.set("https://github.com/outsidesource/OSKit-KMP")
            connection.set("scm:git:git://github.com/outsidesource/OSKit-KMP.git")
            developerConnection.set("scm:git:ssh://git@github.com/outsidesource/OSKit-KMP.git")
        }
        developers {
            developer {
                id.set("ryanmitchener")
                name.set("Ryan Mitchener")
            }
            developer {
                id.set("osddeveloper")
                name.set("Outside Source")
            }
        }
    }
}