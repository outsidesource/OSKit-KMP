package com.outsidesource.oskitkmp.storage.sqldelight

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Unit

internal interface KmpKVStoreDatabase : Transacter {
    public val kmpKVStoreDatabaseQueries: KmpKVStoreDatabaseQueries

    public companion object {
        public val Schema: SqlSchema<QueryResult.Value<Unit>>
            get() = KmpKVStoreDatabase::class.schema

        public operator fun invoke(driver: SqlDriver): KmpKVStoreDatabase =
            KmpKVStoreDatabase::class.newInstance(driver)
    }
}
