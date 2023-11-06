import java.io.File
import java.io.FileInputStream
import java.lang.System.getenv
import java.util.*

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", Versions.Kotlin))
    }
}

plugins {
    kotlin("multiplatform") version Versions.Kotlin
    kotlin("plugin.serialization") version Versions.Kotlin
    id("org.jlleitschuh.gradle.ktlint") version Versions.KtLintPlugin
    id("com.android.library")
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.10"
}

val lwjglVersion = "3.3.2"

val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "FreeBSD", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) }) {
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            } else {
                "natives-linux"
            }
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
        arrayOf("Windows").any { name.startsWith(it) } ->
            if (arch.contains("64")) {
                "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            } else {
                "natives-windows-x86"
            }
        else ->
            throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
    }
}

apply(from = "versioning.gradle.kts")

val versionProperty = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, "version.properties")))
}["version"] ?: "0.0.0"

group = "com.outsidesource"
version = versionProperty

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
}

kotlin {
    jvm {
        jvmToolchain(17)
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    androidTarget {
        jvmToolchain(17)
        publishLibraryVariants("release", "debug")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "oskitkmp"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Dependencies.KotlinxAtomicFu)
                implementation(Dependencies.KotlinxDateTime)
                implementation(Dependencies.CoroutinesCore)
                implementation(Dependencies.KotlinxSerializationJson)
                implementation(Dependencies.KotlinxSerializationCBOR)
                implementation(Dependencies.KtorServerCore)
                implementation(Dependencies.KtorServerCIO)
                implementation(Dependencies.KtorClientCore)
                implementation(Dependencies.KtorClientCIO)
                implementation(Dependencies.KtorWebsockets)
                implementation(Dependencies.OkIO)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.8.0")
                implementation("androidx.documentfile:documentfile:1.0.1")
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.lwjgl:lwjgl-tinyfd:$lwjglVersion")
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:$lwjglNatives")
            }
        }
    }

    afterEvaluate {
        getenv("GITHUB_REPOSITORY")?.let { repoName ->
            publishing {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/$repoName")
                        credentials {
                            username = getenv("OSD_DEVELOPER")
                            password = getenv("OSD_TOKEN")
                        }
                    }
                }
            }
        }
    }
}

android {
    namespace = "com.outsidesource.oskitkmp"
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
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
