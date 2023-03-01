package com.outsidesource.oskitkmp.lib

data class SemVer(val major: Int = 0, val minor: Int = 0, val patch: Int = 0) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (this == other) return 0

        if (this.major > other.major) return 1
        if (this.major < other.major) return -1

        if (this.minor > other.minor) return 1
        if (this.minor < other.minor) return -1

        if (this.patch > other.patch) return 1
        if (this.patch < other.patch) return -1

        return 0
    }

    override fun toString() = "$major.$minor.$patch"

    companion object {
        val Invalid = SemVer()

        fun fromString(version: String): SemVer {
            val versions = version.replace("[^0-9.]".toRegex(), "")
                .split(".")
                .map { it.take(1) }
                .filter { it.isNotBlank() }

            return SemVer(
                versions.getOrNull(0)?.toInt() ?: 0,
                versions.getOrNull(1)?.toInt() ?: 0,
                versions.getOrNull(2)?.toInt() ?: 0
            )
        }

        fun isValidSemVer(version: String): Boolean = version.matches("[0-9]+\\.[0-9]+\\.[0-9]+".toRegex())
    }
}
