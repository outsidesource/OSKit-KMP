package com.outsidesource.oskitkmp.storage.sqldelight

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<KmpKVStoreDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
    get() = KmpKVStoreDatabaseImpl.Schema

internal fun KClass<KmpKVStoreDatabase>.newInstance(driver: SqlDriver): KmpKVStoreDatabase =
    KmpKVStoreDatabaseImpl(driver)

private class KmpKVStoreDatabaseImpl(
    driver: SqlDriver,
) : TransacterImpl(driver), KmpKVStoreDatabase {
    override val kmpKVStoreDatabaseQueries: KmpKVStoreDatabaseQueries =
        KmpKVStoreDatabaseQueries(driver)

    public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long
            get() = 1

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(
                null,
                """
          |CREATE TABLE IF NOT EXISTS kmp_storage (
          |    key TEXT PRIMARY KEY NOT NULL,
          |    value BLOB NOT NULL
          |)
                """.trimMargin(),
                0,
            )
            return QueryResult.Unit
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion,
        ): QueryResult.Value<Unit> = QueryResult.Unit
    }
}
