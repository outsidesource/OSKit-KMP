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
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    android {
        publishLibraryVariants("release", "debug")
    }
    ios {
        binaries {
            framework {
                baseName = "oskitkmp"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Dependencies.KotlinxAtomicFu)
                implementation(Dependencies.KotlinxDateTime)
                implementation(Dependencies.CoroutinesCore)
                implementation(Dependencies.KotlinxSerializationJson)
                implementation(Dependencies.KtorServerCore)
                implementation(Dependencies.KtorServerCIO)
                implementation(Dependencies.KtorClientCore)
                implementation(Dependencies.KtorClientCIO)
                implementation(Dependencies.KtorWebsockets)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {}
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val jvmMain by getting {
            dependencies {}
        }
        val jvmTest by getting
        val iosMain by getting
        val iosTest by getting
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
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
