package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KmpKVStoreDatabase

class IosKmpKVStore : IKmpKVStore {
    override fun openNode(nodeName: String): Outcome<IKmpKVStoreNode, Exception> = try {
        Outcome.Ok(KmpKVStoreNode(KmpKVStoreContext(), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual class KmpKVStoreContext

internal actual fun createDatabaseDriver(context: KmpKVStoreContext, nodeName: String): SqlDriver {
    return NativeSqliteDriver(KmpKVStoreDatabase.Companion.Schema, "$nodeName.db")
}
