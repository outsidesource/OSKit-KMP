package com.outsidesource.oskitkmp.storage

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.outsidesource.oskitkmp.outcome.Outcome

class AndroidKMPStorage(private val appContext: Context) : IKMPStorage {
    override fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception> = try {
        Outcome.Ok(KMPStorageNode(KMPStorageContext(appContext), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual data class KMPStorageContext(val appContext: Context)

internal actual fun createDatabaseDriver(context: KMPStorageContext, nodeName: String): SqlDriver {
    return AndroidSqliteDriver(KMPStorageDatabase.Schema, context.appContext, "$nodeName.db")
}
