package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.outsidesource.oskitkmp.filesystem.FsUtil
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KmpKvStoreDatabase
import java.io.File

fun KmpKvStore(appName: String): IKmpKvStore = JvmKmpKvStore(appName)

internal class JvmKmpKvStore(private val appName: String) : IKmpKvStore {
    override suspend fun openNode(nodeName: String): Outcome<IKmpKvStoreNode, Exception> = try {
        Outcome.Ok(KmpKvStoreNode(KmpKvStoreContext(appName), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual data class KmpKvStoreContext(val appName: String)

internal actual fun createDatabaseDriver(context: KmpKvStoreContext, nodeName: String): SqlDriver {
    // Make app folder
    File(FsUtil.appDirPath(context.appName)).mkdirs()

    val fileName = "${FsUtil.appDirPath(context.appName)}/$nodeName.db"
    val driver = JdbcSqliteDriver("jdbc:sqlite:$fileName")
    KmpKvStoreDatabase.Companion.Schema.create(driver)
    return driver
}
