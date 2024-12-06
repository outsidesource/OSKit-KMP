package com.outsidesource.oskitkmp.filesystem

object FileUtil {
    fun appDirPath(appName: String): String = "${System.getProperty("user.home")}/.$appName"
}
