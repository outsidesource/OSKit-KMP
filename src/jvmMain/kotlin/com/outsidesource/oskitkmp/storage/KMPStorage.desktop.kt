package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.outsidesource.oskitkmp.file.FileUtil
import java.io.File

actual data class KMPStorageContext(override val appName: String, val dbLocation: String? = null) : IKMPStorageContext

internal actual fun createDatabaseDriver(context: KMPStorageContext): SqlDriver {
    // Make app folder
    File(FileUtil.appDirPath(context.appName)).mkdirs()

    val fileName = context.dbLocation ?: "${FileUtil.appDirPath(context.appName)}/${context.appName}.db"
    val driver = JdbcSqliteDriver("jdbc:sqlite:$fileName")
    KMPStorageDatabase.Schema.create(driver)
    return driver
}
