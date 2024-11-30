package com.outsidesource.oskitkmp.storage.sqldelight

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Unit

internal interface KmpKvStoreDatabase : Transacter {
    public val kmpKvStoreDatabaseQueries: KmpKvStoreDatabaseQueries

    public companion object {
        public val Schema: SqlSchema<QueryResult.Value<Unit>>
            get() = KmpKvStoreDatabase::class.schema

        public operator fun invoke(driver: SqlDriver): KmpKvStoreDatabase =
            KmpKvStoreDatabase::class.newInstance(driver)
    }
}
