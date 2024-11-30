package com.outsidesource.oskitkmp.storage

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KmpKvStoreDatabase

class AndroidKmpKvStore(private val appContext: Context) : IKmpKvStore {
    override suspend fun openNode(nodeName: String): Outcome<IKmpKvStoreNode, Exception> = try {
        Outcome.Ok(KmpKvStoreNode(KmpKvStoreContext(appContext), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual data class KmpKvStoreContext(val appContext: Context)

internal actual fun createDatabaseDriver(context: KmpKvStoreContext, nodeName: String): SqlDriver {
    return AndroidSqliteDriver(KmpKvStoreDatabase.Companion.Schema, context.appContext, "$nodeName.db")
}
