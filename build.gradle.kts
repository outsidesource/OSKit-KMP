@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.io.FileInputStream
import java.util.*

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", libs.versions.kotlin.toString()))
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://plugins.gradle.org/m2/")
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.dokka)
    id("com.android.library")
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

    androidTarget()

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
        val androidInstrumentedTest by getting {
            dependsOn(commonTest)
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
                implementation("net.java.dev.jna:jna:5.17.0")
                implementation("net.java.dev.jna:jna-platform:5.17.0")
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

android {
    namespace = "com.outsidesource.oskitkmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ktlint {
    debug.set(true)
    disabledRules.set(setOf("no-wildcard-imports", "filename"))

    filter {
        include("src/**/*.kt")
        exclude("**/*.kts")
        val excludedDirs = listOf("/generated/", "/commonTest/", "/androidTest/", "/iosTest/", "/jvmTest/")
        exclude { tree -> excludedDirs.any { projectDir.toURI().relativize(tree.file.toURI()).path.contains(it) } }
    }
}

tasks.getByName("preBuild").dependsOn("ktlintFormat")

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    configure(
        platform = KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
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