package com.outsidesource.oskitkmp.filesystem

object FsUtil {
    fun appDirPath(appName: String): String = "${System.getProperty("user.home")}/.$appName"
}
