package com.outsidesource.oskitkmp.storage

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual data class KMPStorageContext(override val appName: String, val appContext: Context) : IKMPStorageContext

internal actual fun createDatabaseDriver(context: KMPStorageContext, nodeName: String): SqlDriver {
    return AndroidSqliteDriver(KMPStorageDatabase.Schema, context.appContext, "$nodeName.db")
}
