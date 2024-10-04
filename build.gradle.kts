import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import java.io.FileInputStream
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
    id("com.vanniktech.maven.publish") version "0.28.0"
    id("app.cash.sqldelight") version "2.0.2"
}

sqldelight {
    databases {
        create("KMPStorageDatabase") {
            packageName.set("com.outsidesource.oskitkmp.storage")
        }
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
    mavenCentral()
    gradlePluginPortal()
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
                implementation("app.cash.sqldelight:android-driver:2.0.2")
                implementation("androidx.activity:activity-compose:1.9.2")
                implementation("androidx.lifecycle:lifecycle-runtime:2.8.6")
                implementation("androidx.documentfile:documentfile:1.0.1")
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation("androidx.test:runner:1.5.2")
                implementation("androidx.test:core:1.5.0")
                implementation("junit:junit:4.13.2")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.2")
            }
        }
        val iosTest by getting
        val jvmMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
                implementation(project.dependencies.platform("org.lwjgl:lwjgl-bom:3.3.3"))
                implementation("org.lwjgl:lwjgl")
                implementation("org.lwjgl:lwjgl-tinyfd")
            }
        }
        val jvmTest by getting
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
    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
//    signAllPublications()

    configure(
        platform = KotlinMultiplatform(
//            javadocJar = JavadocJar.Dokka("dokkaHtml"),
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