package com.outsidesource.oskitkmp.file

object FileUtil {
    fun appDirPath(appName: String): String = "${System.getProperty("user.home")}/.$appName"
}
