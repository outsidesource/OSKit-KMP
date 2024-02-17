package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual data class KMPStorageContext(override val appName: String) : IKMPStorageContext

internal actual fun createDatabaseDriver(context: KMPStorageContext): SqlDriver {
    return NativeSqliteDriver(KMPStorageDatabase.Schema, "${context.appName}.db")
}
