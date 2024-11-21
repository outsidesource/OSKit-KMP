package com.outsidesource.oskitkmp.storage.sqldelight

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Unit

internal interface KMPStorageDatabase : Transacter {
    public val kMPStorageDatabaseQueries: KMPStorageDatabaseQueries

    public companion object {
        public val Schema: SqlSchema<QueryResult.Value<Unit>>
            get() = KMPStorageDatabase::class.schema

        public operator fun invoke(driver: SqlDriver): KMPStorageDatabase =
            KMPStorageDatabase::class.newInstance(driver)
    }
}
