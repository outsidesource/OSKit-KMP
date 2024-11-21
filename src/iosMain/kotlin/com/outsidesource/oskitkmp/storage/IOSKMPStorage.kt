package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KMPStorageDatabase

class IOSKMPStorage : IKMPStorage {
    override fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception> = try {
        Outcome.Ok(KMPStorageNode(KMPStorageContext(), nodeName))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal actual class KMPStorageContext

internal actual fun createDatabaseDriver(context: KMPStorageContext, nodeName: String): SqlDriver {
    return NativeSqliteDriver(KMPStorageDatabase.Companion.Schema, "$nodeName.db")
}
