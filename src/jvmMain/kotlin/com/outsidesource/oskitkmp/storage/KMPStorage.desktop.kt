package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.outsidesource.oskitkmp.file.FileUtil
import java.io.File

actual data class KMPStorageContext(override val appName: String, val dbDirectory: String? = null) : IKMPStorageContext

internal actual fun createDatabaseDriver(context: KMPStorageContext, nodeName: String): SqlDriver {
    // Make app folder
    File(FileUtil.appDirPath(context.appName)).mkdirs()

    val fileName = if (context.dbDirectory != null) {
        "${context.dbDirectory}/$nodeName.db"
    } else {
        "${FileUtil.appDirPath(context.appName)}/$nodeName.db"
    }

    val driver = JdbcSqliteDriver("jdbc:sqlite:$fileName")
    KMPStorageDatabase.Schema.create(driver)
    return driver
}
