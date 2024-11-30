package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KmpKvStoreDatabase

class IosKmpKvStore : IKmpKvStore {
    override suspend fun openNode(nodeName: String): Outcome<IKmpKvStoreNode, Exception> = try {
        Outcome.Ok(KmpKvStoreNode(KmpKvStoreContext(), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual class KmpKvStoreContext

internal actual fun createDatabaseDriver(context: KmpKvStoreContext, nodeName: String): SqlDriver {
    return NativeSqliteDriver(KmpKvStoreDatabase.Companion.Schema, "$nodeName.db")
}
