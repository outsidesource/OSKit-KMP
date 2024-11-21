package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.outsidesource.oskitkmp.file.FileUtil
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KMPStorageDatabase
import java.io.File

class DesktopKMPStorage(private val appName: String) : IKMPStorage {
    override fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception> = try {
        Outcome.Ok(KMPStorageNode(KMPStorageContext(appName), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual data class KMPStorageContext(val appName: String)

internal actual fun createDatabaseDriver(context: KMPStorageContext, nodeName: String): SqlDriver {
    // Make app folder
    File(FileUtil.appDirPath(context.appName)).mkdirs()

    val fileName = "${FileUtil.appDirPath(context.appName)}/$nodeName.db"
    val driver = JdbcSqliteDriver("jdbc:sqlite:$fileName")
    KMPStorageDatabase.Companion.Schema.create(driver)
    return driver
}
