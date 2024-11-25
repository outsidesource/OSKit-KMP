package com.outsidesource.oskitkmp.storage

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KmpKVStoreDatabase

class AndroidKmpKVStore(private val appContext: Context) : IKmpKVStore {
    override suspend fun openNode(nodeName: String): Outcome<IKmpKVStoreNode, Exception> = try {
        Outcome.Ok(KmpKVStoreNode(KmpKVStoreContext(appContext), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual data class KmpKVStoreContext(val appContext: Context)

internal actual fun createDatabaseDriver(context: KmpKVStoreContext, nodeName: String): SqlDriver {
    return AndroidSqliteDriver(KmpKVStoreDatabase.Companion.Schema, context.appContext, "$nodeName.db")
}
