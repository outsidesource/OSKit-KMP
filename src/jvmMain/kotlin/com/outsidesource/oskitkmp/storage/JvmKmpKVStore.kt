package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.outsidesource.oskitkmp.file.FileUtil
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KmpKVStoreDatabase
import java.io.File

class JvmKmpKVStore(private val appName: String) : IKmpKVStore {
    override suspend fun openNode(nodeName: String): Outcome<IKmpKVStoreNode, Exception> = try {
        Outcome.Ok(KmpKVStoreNode(KmpKVStoreContext(appName), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual data class KmpKVStoreContext(val appName: String)

internal actual fun createDatabaseDriver(context: KmpKVStoreContext, nodeName: String): SqlDriver {
    // Make app folder
    File(FileUtil.appDirPath(context.appName)).mkdirs()

    val fileName = "${FileUtil.appDirPath(context.appName)}/$nodeName.db"
    val driver = JdbcSqliteDriver("jdbc:sqlite:$fileName")
    KmpKVStoreDatabase.Companion.Schema.create(driver)
    return driver
}
