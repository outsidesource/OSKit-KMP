import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

fun readVersion(): Properties {
    val versionFile = File(project.rootDir, "version.properties")

    var version = Properties()

    FileInputStream(versionFile).use { stream ->
        version.load(stream)
    }

    return version
}

fun readVersionActual() {
    val version = readVersion()
    println("VERSION: ${version["version"]}")
}

fun incrementMajorVersion() {
    val versionFile = File(project.rootDir, "version.properties")
    val version = readVersion()

    var major = (version["major"] as String).toInt()
    major++

    version.setProperty("major", major.toString())
    version.setProperty("minor", "0")
    version.setProperty("revision", "0")
    version.setProperty("version", "${version["major"]}.${version["minor"]}.${version["revision"]}")

    FileOutputStream(versionFile).use { stream ->
        version.store(stream, null)
    }

    println("Major Version has been updated to: ${version["version"]}")
}

fun incrementMinorVersion() {
    val versionFile = File(project.rootDir, "version.properties")
    val version = readVersion()

    var minor = (version["minor"] as String).toInt()
    minor++

    version.setProperty("minor", minor.toString())
    version.setProperty("revision", "0")
    version.setProperty("version", "${version["major"]}.${version["minor"]}.${version["revision"]}")

    FileOutputStream(versionFile).use { stream ->
        version.store(stream, null)
    }

    println("Major Version has been updated to: ${version["version"]}")

}

fun incrementRevisionVersion() {
    val versionFile = File(project.rootDir, "version.properties")
    val version = readVersion()

    var revision = (version["revision"] as String).toInt()
    revision++

    version.setProperty("revision", revision.toString())
    version.setProperty("version", "${version["major"]}.${version["minor"]}.${version["revision"]}")

    FileOutputStream(versionFile).use { stream ->
        version.store(stream, null)
    }

    println("Major Version has been updated to: ${version["version"]}")
}

fun incrementBuildNumber(buildNumber: String) {
    val versionFile = File(project.rootDir, "version.properties")
    val version = readVersion()

    version.setProperty("build", buildNumber)
    FileOutputStream(versionFile).use { stream ->
        version.store(stream, null)
    }
}

val buildNumber: String? by project
task("incrementBuildNumber") {
    doFirst {
        incrementBuildNumber(buildNumber ?: "0")
    }
}

tasks.register("incrementMajorVersion") {
    doLast {
        incrementMajorVersion()
    }
}

tasks.register("incrementMinorVersion") {
    doLast {
        incrementMinorVersion()
    }
}

tasks.register("incrementRevisionVersion") {
    doLast {
        incrementRevisionVersion()
    }
}

tasks.register("readVersionActual") {
    doLast {
        readVersionActual()
    }
}